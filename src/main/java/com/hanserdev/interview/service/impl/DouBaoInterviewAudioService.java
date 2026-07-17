package com.hanserdev.interview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanserdev.interview.config.InterviewAudioPropertiesDouBao;
import com.hanserdev.interview.messaging.InterviewAudioWebSocketPublisher;
import com.hanserdev.interview.model.vo.interview.AudioStreamMessage;
import com.hanserdev.interview.protocol.EventType;
import com.hanserdev.interview.protocol.Message;
import com.hanserdev.interview.protocol.MsgType;
import com.hanserdev.interview.protocol.SpeechWebSocketClient;
import com.hanserdev.interview.service.InterviewAudioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 使用豆包双向 TTS 实现语音播报
 *
 * @Author Zane
 * @CreateTime 2025/11/19 星期三 22:16
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "interview.audio.tts.enabled", havingValue = "true")
@ConditionalOnProperty(name = "doubao.enabled", havingValue = "true")
public class DouBaoInterviewAudioService implements InterviewAudioService {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final InterviewAudioPropertiesDouBao audioProperties;
    private final InterviewAudioWebSocketPublisher audioWebSocketPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // WebSocket 长连接管理
    private SpeechWebSocketClient client;
    private final Object connectionLock;
    
    public DouBaoInterviewAudioService(InterviewAudioPropertiesDouBao audioProperties, 
                                       InterviewAudioWebSocketPublisher audioWebSocketPublisher) {
        this.audioProperties = audioProperties;
        this.audioWebSocketPublisher = audioWebSocketPublisher;
        this.connectionLock = new Object();
    }

    @Override
    @Async("TaskExecutor")
    public void streamInterviewerSpeech(UUID sessionId, int sequenceNumber, String text) {
        if (!audioProperties.isEnabled()) {
            log.debug("语音播报未开启，跳过本次推流: sessionId={}, seq={}", sessionId, sequenceNumber);
            return;
        }
        if (StringUtils.isBlank(text)) {
            log.debug("语音内容为空，跳过播报: sessionId={}, seq={}", sessionId, sequenceNumber);
            return;
        }

        String speechText = prepareSpeechText(text);
        if (StringUtils.isBlank(speechText)) {
            log.debug("Markdown 预处理后语音内容为空，跳过播报: sessionId={}, seq={}", sessionId, sequenceNumber);
            return;
        }

        if (StringUtils.isBlank(audioProperties.getAppId()) || StringUtils.isBlank(audioProperties.getAccessToken())) {
            log.warn("缺少豆包 App ID 或 Access Token，无法进行语音合成");
            publishErrorEvent(sessionId, sequenceNumber, "豆包配置不完整");
            return;
        }

        publishStartEvent(sessionId, sequenceNumber, speechText);

        try {
            performTTS(sessionId, sequenceNumber, speechText);
            publishCompleteEvent(sessionId, sequenceNumber);
        } catch (Exception e) {
            log.error("语音播报失败: sessionId={}, seq={}", sessionId, sequenceNumber, e);
            publishErrorEvent(sessionId, sequenceNumber, "语音生成失败，请稍后重试");
        }
    }

    /**
     * 执行 TTS 合成。
     */
    private void performTTS(UUID sessionId, int sequenceNumber, String text) throws Exception {
        log.info("开始执行豆包 TTS: sessionId={}, voice={}, encoding={}, resourceId={}",
                sessionId, audioProperties.getVoice(), audioProperties.getEncoding(), audioProperties.getResourceId());

        // 确保连接已建立
        ensureConnected();
        log.info("豆包 WebSocket 连接已就绪");

        // 构建请求
        Map<String, Object> request = Map.of(
                "user", Map.of("uid", UUID.randomUUID().toString()),
                "namespace", "BidirectionalTTS",
                "req_params", Map.of(
                        "text", text,
                        "model", "seed-tts-1.1",
                        "speaker", audioProperties.getVoice(),
                        "audio_params", Map.of(
                                "format", audioProperties.getEncoding(),
                                "sample_rate", 24000,
                                "emotion", "coldness",
                                "speech_rate", audioProperties.getSpeechRate()),
                        "additions", objectMapper.writeValueAsString(Map.of(
                                "disable_markdown_filter", false, // 解析并过滤markdown语法
                                "use_tag_parser", true)))); // 辅助当前语音合成，对语速、情感等进行调整

        // 处理每个句子
        String[] sentences = text.split("。");
        int chunkIndex = 0;
        for (int i = 0; i < sentences.length; i++) {
            if (sentences[i].trim().isEmpty()) {
                continue;
            }

            String sessionIdStr = UUID.randomUUID().toString();

            // 启动会话
            log.info("启动会话: {}", sessionIdStr);
            Map<String, Object> startReq = Map.of(
                    "user", request.get("user"),
                    "req_params", request.get("req_params"),
                    "namespace", request.get("namespace"),
                    "event", EventType.START_SESSION.getValue());
            synchronized (connectionLock) {
                client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionIdStr);
            }
            log.info("等待 SESSION_STARTED 消息");
            synchronized (connectionLock) {
                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);
            }
            log.info("会话已启动");

            // 发送文本
            log.info("发送文本内容: {}", sentences[i]);
            for (char c : sentences[i].toCharArray()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentReqParams = new HashMap<>(
                        (Map<String, Object>) request.get("req_params"));
                currentReqParams.put("text", String.valueOf(c));

                Map<String, Object> currentRequest = Map.of(
                        "user", request.get("user"),
                        "namespace", request.get("namespace"),
                        "req_params", currentReqParams,
                        "event", EventType.TASK_REQUEST.getValue());

                synchronized (connectionLock) {
                    client.sendTaskRequest(objectMapper.writeValueAsBytes(currentRequest), sessionIdStr);
                }
            }
            log.info("文本发送完成");

            // 结束会话
            log.info("结束会话");
            synchronized (connectionLock) {
                client.sendFinishSession(sessionIdStr);
            }

            // 接收响应
            log.info("开始接收音频消息");
            int audioChunkCount = 0;
            while (true) {
                Message msg;
                synchronized (connectionLock) {
                    msg = client.receiveMessage();
                }
                log.debug("接收到消息: type={}, event={}", msg.getType(), msg.getEvent());
                switch (msg.getType()) {
                    case FULL_SERVER_RESPONSE:
                        break;
                    case AUDIO_ONLY_SERVER:
                        if (msg.getPayload() != null) {
                            audioChunkCount++;
                            log.debug("接收到音频块: size={}", msg.getPayload().length);
                            // 推送音频块
                            String base64Audio = java.util.Base64.getEncoder().encodeToString(msg.getPayload());
                            audioWebSocketPublisher.publish(sessionId, AudioStreamMessage.builder()
                                    .sessionId(sessionId)
                                    .sequenceNumber(sequenceNumber)
                                    .type(AudioStreamMessage.Type.CHUNK)
                                    .chunkIndex(chunkIndex++)
                                    .audioFormat(audioProperties.getEncoding().toUpperCase())
                                    .voice(audioProperties.getVoice())
                                    .base64Audio(base64Audio)
                                    .timestamp(System.currentTimeMillis())
                                    .build());
                        }
                        break;
                    default:
                        throw new RuntimeException("意外的消息: " + msg);
                }
                if (msg.getEvent() == EventType.SESSION_FINISHED) {
                    log.info("会话结束, 接收到 {} 个音频块", audioChunkCount);
                    break;
                }
            }
        }

        log.info("豆包 TTS 执行完成");
    }

    /**
     * 确保 WebSocket 连接已建立
     */
    private void ensureConnected() throws Exception {
        synchronized (connectionLock) {
            if (client != null && client.isOpen()) {
                log.debug("WebSocket 连接已存在");
                return;
            }

            log.info("正在建立豆包 WebSocket 连接: {}", audioProperties.getWsEndpoint());
            Map<String, String> headers = Map.of(
                    "X-Api-App-Key", audioProperties.getAppId(),
                    "X-Api-Access-Key", audioProperties.getAccessToken(),
                    "X-Api-Resource-Id", audioProperties.getResourceId().isEmpty()
                            ? voiceToResourceId(audioProperties.getVoice())
                            : audioProperties.getResourceId(),
                    "X-Api-Connect-Id", UUID.randomUUID().toString());

            client = new SpeechWebSocketClient(new URI(audioProperties.getWsEndpoint()), headers);
            client.connectBlocking();
            log.info("豆包 WebSocket 连接成功");
            
            // 发送 START_CONNECTION
            log.info("发送豆包 START_CONNECTION");
            client.sendStartConnection();
            log.info("等待 CONNECTION_STARTED 消息");
            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);
            log.info("豆包连接已启动");
        }
    }

    /**
     * 关闭 WebSocket 连接
     */
    public void closeConnection() throws Exception {
        synchronized (connectionLock) {
            if (client != null && client.isOpen()) {
                log.info("关闭 WebSocket 连接");
                try {
                    client.sendFinishConnection();
                } catch (Exception e) {
                    log.warn("发送 FINISH_CONNECTION 失败", e);
                }
                client.closeBlocking();
                client = null;
            }
        }
    }

    /**
     * 根据语音类型获取 Resource ID
     */
    private String voiceToResourceId(String voice) {
        if (voice.startsWith("S_")) {
            return "volc.megatts.default";
        }
        return "volc.service_type.10029";
    }

    private void publishStartEvent(UUID sessionId, int sequenceNumber, String text) {
        audioWebSocketPublisher.publish(sessionId, AudioStreamMessage.builder()
                .sessionId(sessionId)
                .sequenceNumber(sequenceNumber)
                .type(AudioStreamMessage.Type.START)
                .audioFormat(audioProperties.getEncoding().toUpperCase())
                .voice(audioProperties.getVoice())
                .text(text)
                .message("开始生成语音")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void publishCompleteEvent(UUID sessionId, int sequenceNumber) {
        audioWebSocketPublisher.publish(sessionId, AudioStreamMessage.builder()
                .sessionId(sessionId)
                .sequenceNumber(sequenceNumber)
                .type(AudioStreamMessage.Type.COMPLETE)
                .audioFormat(audioProperties.getEncoding().toUpperCase())
                .voice(audioProperties.getVoice())
                .message("语音生成完成")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void publishErrorEvent(UUID sessionId, int sequenceNumber, String errorMessage) {
        audioWebSocketPublisher.publish(sessionId, AudioStreamMessage.builder()
                .sessionId(sessionId)
                .sequenceNumber(sequenceNumber)
                .type(AudioStreamMessage.Type.ERROR)
                .message(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private String prepareSpeechText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String sanitized = CODE_BLOCK_PATTERN.matcher(text).replaceAll(" ");
        sanitized = IMAGE_PATTERN.matcher(sanitized).replaceAll("$1 ");
        sanitized = LINK_PATTERN.matcher(sanitized).replaceAll("$1");
        sanitized = INLINE_CODE_PATTERN.matcher(sanitized).replaceAll("$1");
        sanitized = sanitized.replaceAll("(?m)^\\s{0,3}(?:#{1,6})\\s*", "");
        sanitized = sanitized.replaceAll("~~(.*?)~~", "$1");
        sanitized = sanitized.replaceAll("(\\*\\*|__)(.*?)\\1", "$2");
        sanitized = sanitized.replaceAll("(\\*|_)(.*?)\\1", "$2");
        sanitized = sanitized.replaceAll("(?m)^\\s{0,3}(?:[-*+]|\\d+\\.)\\s+", "");
        sanitized = sanitized.replaceAll("(?m)^>+\\s*", "");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = sanitized.replace('|', ' ');
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        return StringUtils.defaultIfBlank(sanitized, text).trim();
    }
}
