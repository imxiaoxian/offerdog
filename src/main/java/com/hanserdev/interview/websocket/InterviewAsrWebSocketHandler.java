package com.hanserdev.interview.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hanserdev.interview.config.InterviewAsrProperties;
import com.hanserdev.interview.service.WhisperAsrService;
import com.hanserdev.interview.utils.AudioConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 前端音频 WebSocket -> 火山引擎 ASR -> 前端文字 的桥接处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAsrWebSocketHandler extends AbstractWebSocketHandler {

    private static final byte PROTOCOL_VERSION = 0b0001;
    private static final byte DEFAULT_HEADER_SIZE = 0b0001;

    private static final byte CLIENT_FULL_REQUEST = 0b0001;
    private static final byte CLIENT_AUDIO_ONLY_REQUEST = 0b0010;
    private static final byte SERVER_FULL_RESPONSE = 0b1001;
    private static final byte SERVER_ERROR_RESPONSE = 0b1111;

    private static final byte POS_SEQUENCE = 0b0001;
    private static final byte NEG_WITH_SEQUENCE = 0b0011;

    private static final byte JSON = 0b0001;
    private static final byte GZIP = 0b0001;

    private final InterviewAsrProperties asrProperties;

    private final WhisperAsrService whisperAsrService;

    @Qualifier("asrOkHttpClient")
    private final OkHttpClient okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    /**
     * Whisper 伪实时转写线程池：避免阻塞 WebSocket IO 线程。
     * 单机默认 2 线程，足够覆盖少量并发会话；需要更高并发可改成可配置。
     */
    private final ExecutorService whisperPartialExecutor = Executors.newFixedThreadPool(2);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!asrProperties.isEnabled()) {
            log.warn("[ASR-WS] ASR disabled, closing session {}", session.getId());
            session.close(new CloseStatus(4400, "ASR disabled"));
            return;
        }

        String provider = asrProperties.getProvider();
        if ("funasr".equalsIgnoreCase(provider)) {
            if (StringUtils.isBlank(asrProperties.getFunasrUrl())) {
                log.warn("[ASR-WS] Missing FunASR URL, closing session {}", session.getId());
                session.close(new CloseStatus(4402, "FunASR URL missing"));
                return;
            }
        } else if ("whisper".equalsIgnoreCase(provider)) {
            // Whisper 走 HTTP 转写，不依赖火山 appId/token
        } else {
            if (StringUtils.isAnyBlank(asrProperties.getAppId(), asrProperties.getToken())) {
                log.warn("[ASR-WS] Missing appId/token, closing session {}", session.getId());
                session.close(new CloseStatus(4401, "ASR appId/token missing"));
                return;
            }
        }

        log.info("[ASR-WS] frontend connected, sessionId={}, provider={}", session.getId(), provider);
        SessionState state = new SessionState();
        state.pcmSampleRate = resolveDefaultPcmSampleRate();
        sessions.put(session.getId(), state);
        connectToBackend(session, state);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[ASR-WS] text message from {}: {}", session.getId(), payload);

        SessionState state = sessions.get(session.getId());
        if (state == null) {
            return;
        }

        if ("END".equalsIgnoreCase(payload)) {
            String provider = asrProperties.getProvider();
            if ("whisper".equalsIgnoreCase(provider)) {
                try {
                    sendWhisperTranscription(session, state);
                } catch (Exception e) {
                    log.error("[Whisper] Transcription error: {}", e.getMessage());
                    sendErrorToFrontend(session, "Whisper transcription failed: " + e.getMessage());
                }
            } else {
                sendLastAudioChunk(state);
            }
            return;
        }

        try {
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(payload);
            String type = jsonNode.path("type").asText();
            if ("init".equalsIgnoreCase(type)) {
                state.businessSessionId = jsonNode.path("sessionId").asText(null);
                int sr = jsonNode.path("sampleRate").asInt(0);
                if (sr > 0) {
                    state.pcmSampleRate = sr;
                }
                log.info("ASR 会话映射: frontWs={} -> businessSessionId={}, pcmSampleRate={}",
                        session.getId(), state.businessSessionId, state.pcmSampleRate);
            } else if ("end".equalsIgnoreCase(type)) {
                String provider = asrProperties.getProvider();
                if ("whisper".equalsIgnoreCase(provider)) {
                    try {
                        sendWhisperTranscription(session, state);
                    } catch (Exception e) {
                        log.error("[Whisper] Transcription error: {}", e.getMessage());
                        sendErrorToFrontend(session, "Whisper transcription failed: " + e.getMessage());
                    }
                } else {
                    sendLastAudioChunk(state);
                }
            }
        } catch (Exception ex) {
            log.debug("文本消息不是 JSON，忽略: {}", ex.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SessionState state = sessions.get(session.getId());
        if (state == null) {
            log.warn("[ASR-WS] session not found: {}", session.getId());
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] audioData = new byte[payload.remaining()];
        payload.get(audioData);
        if (audioData.length == 0) {
            log.warn("[ASR-WS] empty audio chunk from {}", session.getId());
            return;
        }

        String provider = asrProperties.getProvider();
        
        if ("whisper".equalsIgnoreCase(provider)) {
            handleWhisperAudio(session, state, audioData);
        } else if ("funasr".equalsIgnoreCase(provider)) {
            if (state.backendWebSocket == null) {
                log.warn("[ASR-WS] backend not ready for session {}", session.getId());
                sendErrorToFrontend(session, "Backend connection not ready");
                return;
            }
            sendAudioToFunasr(state.backendWebSocket, audioData);
        } else {
            if (state.backendWebSocket == null) {
                log.warn("[ASR-WS] backend not ready for session {}", session.getId());
                sendErrorToFrontend(session, "Backend connection not ready");
                return;
            }
            if (state.messageSequence == 0) {
                state.messageSequence = 1;
                sendFullClientRequest(state.backendWebSocket, state.messageSequence);
            }

            state.messageSequence++;
            int seq = state.messageSequence;
            byte[] wavData = AudioConverter.pcmToWav(audioData, state.pcmSampleRate);
            log.info("[ASR-WS] forward audio seq={}, wavSize={}, rawSize={}, session={}",
                    seq, wavData.length, audioData.length, session.getId());
            sendAudioSegment(state.backendWebSocket, wavData, false, seq);
        }
    }

    private void handleWhisperAudio(WebSocketSession session, SessionState state, byte[] audioData) {
        try {
            synchronized (state) {
                state.audioBuffer.write(audioData);
            }
        } catch (IOException e) {
            log.error("[Whisper] Failed to write audio to buffer: {}", e.getMessage());
        }
        log.debug("[Whisper] Buffered audio, total size: {}", state.audioBuffer.size());

        if (!asrProperties.isWhisperPseudoRealtimeEnabled()) {
            return;
        }
        maybeScheduleWhisperPartial(session, state);
    }

    private void maybeScheduleWhisperPartial(WebSocketSession session, SessionState state) {
        if (session == null || !session.isOpen()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - state.lastPartialAtMs < asrProperties.getWhisperPseudoRealtimeMinIntervalMs()) {
            return;
        }
        if (!state.partialTranscribing.compareAndSet(false, true)) {
            return;
        }

        final int pcmSampleRate;
        final byte[] snapshot;
        synchronized (state) {
            pcmSampleRate = state.pcmSampleRate;
            int thresholdBytes = bytesForPcmMs(pcmSampleRate, asrProperties.getWhisperPseudoRealtimeChunkMs());
            if (state.audioBuffer.size() < thresholdBytes) {
                state.partialTranscribing.set(false);
                return;
            }
            snapshot = state.audioBuffer.toByteArray();
        }

        state.lastPartialAtMs = now;
        final String businessSessionId = StringUtils.defaultIfBlank(state.businessSessionId, session.getId());

        whisperPartialExecutor.execute(() -> {
            try {
                byte[] forWhisper = AudioConverter.isWavFormat(snapshot)
                        ? snapshot
                        : AudioConverter.pcmToWav(snapshot, pcmSampleRate);

                String text = whisperAsrService.transcribeSync(forWhisper, asrProperties.getWhisperLanguage());
                text = text == null ? "" : text.trim();

                // 只发送“增量”，避免每次都把全文覆盖输入框
                String delta = computeDelta(state.lastPartialText, text);
                if (StringUtils.isNotBlank(delta) && session.isOpen()) {
                    sendTranscript(session, businessSessionId, delta, false);
                }

                if (StringUtils.isNotBlank(text)) {
                    state.lastPartialText = text;
                }

                // Trim buffer: keep overlap + any new audio appended while transcribing
                int overlapBytes = bytesForPcmMs(pcmSampleRate, asrProperties.getWhisperPseudoRealtimeOverlapMs());
                synchronized (state) {
                    byte[] cur = state.audioBuffer.toByteArray();
                    int cutFrom = Math.max(0, snapshot.length - overlapBytes);
                    if (cur.length >= snapshot.length) {
                        byte[] keep = Arrays.copyOfRange(cur, cutFrom, cur.length);
                        state.audioBuffer.reset();
                        state.audioBuffer.write(keep);
                    }
                }
            } catch (Exception e) {
                log.debug("[Whisper] partial transcription failed: {}", e.getMessage());
            } finally {
                state.partialTranscribing.set(false);
            }
        });
    }

    private static String computeDelta(String prev, String current) {
        String p = prev == null ? "" : prev.trim();
        String c = current == null ? "" : current.trim();
        if (c.isEmpty()) return "";
        if (p.isEmpty()) return c;
        if (c.startsWith(p)) {
            return c.substring(p.length()).trim();
        }
        // fallback: if not prefix, return the whole current chunk (still better than nothing)
        return c;
    }

    private static int bytesForPcmMs(int sampleRateHz, int ms) {
        int sr = Math.max(8000, sampleRateHz);
        int durationMs = Math.max(200, ms);
        // 16-bit mono PCM => 2 bytes per sample
        long bytes = (long) sr * 2L * durationMs / 1000L;
        if (bytes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) bytes;
    }

    private void sendAudioToFunasr(WebSocket webSocket, byte[] audioData) {
        webSocket.send(ByteString.of(audioData));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionState state = sessions.remove(session.getId());
        if (state != null) {
            String provider = asrProperties.getProvider();
            if ("whisper".equalsIgnoreCase(provider)) {
                if (state.audioBuffer != null) {
                    try {
                        state.audioBuffer.close();
                    } catch (IOException e) {
                        log.debug("Close audio buffer failed: {}", e.getMessage());
                    }
                }
            } else if (state.backendWebSocket != null) {
                state.backendWebSocket.close(1000, "frontend closed");
            }
        }
        log.info("[ASR-WS] frontend closed session={}, status={}", session.getId(), status);
    }

    private void connectToBackend(WebSocketSession frontendSession, SessionState state) {
        String provider = asrProperties.getProvider();
        if ("whisper".equalsIgnoreCase(provider)) {
            log.info("[ASR-WS] Using Whisper provider, frontend session={}", frontendSession.getId());
            sendStatus(frontendSession, "backend_connected", "Whisper channel ready");
        } else if ("funasr".equalsIgnoreCase(provider)) {
            connectToFunasr(frontendSession, state);
        } else {
            connectToVolcengine(frontendSession, state);
        }
    }

    private void connectToFunasr(WebSocketSession frontendSession, SessionState state) {
        Request request = new Request.Builder()
                .url(asrProperties.getFunasrUrl())
                .build();

        state.backendWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("[ASR-WS] FunASR connected, frontSession={}", frontendSession.getId());
                sendStatus(frontendSession, "backend_connected", "FunASR channel ready");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    log.info("[ASR-WS] FunASR response: {}", text);
                    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(text);
                    String type = jsonNode.path("type").asText();

                    if ("final_result".equals(type) || "partial_result".equals(type)) {
                        sendTranscript(
                                frontendSession,
                                StringUtils.defaultIfBlank(state.businessSessionId, frontendSession.getId()),
                                jsonNode.path("text").asText(),
                                "final_result".equals(type)
                        );
                    }
                } catch (Exception ex) {
                    log.error("处理 FunASR 响应失败", ex);
                    sendErrorToFrontend(frontendSession, "FunASR response parse failed: " + ex.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("[ASR-WS] FunASR failure: {}, frontSession={}", t.getMessage(), frontendSession.getId());
                sendErrorToFrontend(frontendSession, "FunASR backend failed: " + t.getMessage());
            }
        });
    }

    private void connectToVolcengine(WebSocketSession frontendSession, SessionState state) {
        Request request = new Request.Builder()
                .url(asrProperties.getApiUrl())
                .header("X-Api-App-Key", asrProperties.getAppId())
                .header("X-Api-Access-Key", asrProperties.getToken())
                .header("X-Api-Resource-Id", asrProperties.getResourceId())
                .header("X-Api-Connect-Id", UUID.randomUUID().toString())
                .build();

        state.backendWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("[ASR-WS] volcengine connected, logid={}, frontSession={}",
                        response.header("X-Tt-Logid"), frontendSession.getId());
                sendStatus(frontendSession, "backend_connected", "ASR channel ready");
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    AsrResponse response = parseResponse(bytes.toByteArray());
                    log.info("[ASR-WS] volcengine response: {}", response);

                    if (response.payloadMsg != null && !response.payloadMsg.isEmpty()) {
                        forwardTranscript(frontendSession, state, response);
                    }

                    if (response.isLastPackage) {
                        webSocket.close(1000, "finished");
                    }
                } catch (Exception ex) {
                    log.error("处理 ASR 响应失败", ex);
                    sendErrorToFrontend(frontendSession, "ASR response parse failed: " + ex.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("[ASR-WS] volcengine failure: {}, frontSession={}", t.getMessage(), frontendSession.getId());
                sendErrorToFrontend(frontendSession, "ASR backend failed: " + t.getMessage());
            }
        });
    }

    private void forwardTranscript(WebSocketSession frontendSession, SessionState state, AsrResponse response) throws IOException {
        sendTranscript(frontendSession,
                StringUtils.defaultIfBlank(state.businessSessionId, frontendSession.getId()),
                response.payloadMsg,
                response.isLastPackage);
    }

    private void sendTranscript(WebSocketSession session, String sessionId, String text, boolean isFinal) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        String safeText = text == null ? "" : text;
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "transcript");
        payload.put("sessionId", StringUtils.defaultIfBlank(sessionId, session.getId()));
        payload.put("text", safeText);
        payload.put("isFinal", isFinal);
        payload.put("timestamp", System.currentTimeMillis());
        synchronized (session) {
            session.sendMessage(new TextMessage(payload.toString()));
        }
    }

    private void sendStatus(WebSocketSession session, String type, String message) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", type);
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            synchronized (session) {
                session.sendMessage(new TextMessage(payload.toString()));
            }
        } catch (Exception ex) {
            log.debug("发送状态消息失败: {}", ex.getMessage());
        }
    }

    private void sendErrorToFrontend(WebSocketSession session, String error) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", "error");
            payload.put("message", error);
            payload.put("timestamp", System.currentTimeMillis());
            synchronized (session) {
                session.sendMessage(new TextMessage(payload.toString()));
            }
        } catch (Exception ex) {
            log.debug("发送错误消息失败: {}", ex.getMessage());
        }
    }

    private int resolveDefaultPcmSampleRate() {
        if ("whisper".equalsIgnoreCase(asrProperties.getProvider())
                && asrProperties.getWhisperDefaultPcmSampleRate() > 0) {
            return asrProperties.getWhisperDefaultPcmSampleRate();
        }
        return asrProperties.getSampleRate();
    }

    /**
     * 非 PCM 的常见容器头（前端误用 MediaRecorder 时会出现）。
     */
    private static boolean isLikelyCompressedMedia(byte[] b) {
        if (b == null || b.length < 4) {
            return false;
        }
        if ((b[0] & 0xFF) == 0x1a && b[1] == 0x45 && b[2] == (byte) 0xdf && b[3] == (byte) 0xa3) {
            return true;
        }
        if (b[0] == 'O' && b[1] == 'g' && b[2] == 'g' && b[3] == 'S') {
            return true;
        }
        return b[0] == 'I' && b[1] == 'D' && b[2] == '3';
    }

    private void sendLastAudioChunk(SessionState state) {
        if (state == null) {
            return;
        }
        String provider = asrProperties.getProvider();
        if ("whisper".equalsIgnoreCase(provider)) {
            return;
        }
        if (state.backendWebSocket == null) {
            return;
        }
        if ("funasr".equalsIgnoreCase(provider)) {
            state.backendWebSocket.send("{\"type\":\"end\"}");
        } else {
            state.messageSequence++;
            sendAudioSegment(state.backendWebSocket, new byte[0], true, state.messageSequence);
        }
    }

    private void sendWhisperTranscription(WebSocketSession session, SessionState state) {
        byte[] audioBytes;
        synchronized (state) {
            audioBytes = state.audioBuffer.toByteArray();
        }
        try {
            if (audioBytes.length == 0) {
                log.warn("[Whisper] No audio data to transcribe");
                return;
            }

            if (isLikelyCompressedMedia(audioBytes)) {
                log.warn("[Whisper] Received WebM/Ogg/MP3-like payload; need raw 16-bit mono PCM. size={}", audioBytes.length);
                sendErrorToFrontend(session,
                        "ASR expects raw PCM (16-bit mono), not WebM/MediaRecorder blobs. See docs/volcengine-asr-websocket.md");
                return;
            }

            byte[] forWhisper = AudioConverter.isWavFormat(audioBytes)
                    ? audioBytes
                    : AudioConverter.pcmToWav(audioBytes, state.pcmSampleRate);

            log.info("[Whisper] Starting transcription, pcmSampleRate={}, raw size: {}, payload size: {}",
                    state.pcmSampleRate, audioBytes.length, forWhisper.length);

            String text = whisperAsrService.transcribeSync(forWhisper, asrProperties.getWhisperLanguage());
            sendTranscript(session,
                    state.businessSessionId != null ? state.businessSessionId : session.getId(),
                    text,
                    true);
            
            if (StringUtils.isBlank(text)) {
                log.warn("[Whisper] Empty transcript (check mic volume, END after speech, sampleRate in init, or ASR_WHISPER_DEFAULT_PCM_SAMPLE_RATE=48000)");
            } else {
                log.info("[Whisper] Transcription complete: {}", text);
            }

        } catch (Exception e) {
            log.error("[Whisper] Transcription failed: {}", e.getMessage());
            sendErrorToFrontend(session, "Whisper transcription failed: " + e.getMessage());
        } finally {
            synchronized (state) {
                state.audioBuffer.reset();
            }
            state.lastPartialText = "";
        }
    }

    private void sendFullClientRequest(WebSocket webSocket, int seq) {
        ObjectNode user = objectMapper.createObjectNode();
        user.put("uid", "interview_uid");

        ObjectNode audio = objectMapper.createObjectNode();
        audio.put("format", "wav");
        audio.put("codec", "raw");
        audio.put("rate", asrProperties.getSampleRate());
        audio.put("bits", asrProperties.getBits());
        audio.put("channel", asrProperties.getChannel());

        ObjectNode request = objectMapper.createObjectNode();
        request.put("model_name", asrProperties.getModelName());
        request.put("enable_itn", asrProperties.isEnableItn());
        request.put("enable_punc", asrProperties.isEnablePunc());
        request.put("enable_ddc", asrProperties.isEnableDdc());
        request.put("show_utterances", asrProperties.isShowUtterances());
        request.put("enable_nonstream", asrProperties.isEnableNonstream());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("user", user);
        payload.set("audio", audio);
        payload.set("request", request);

        byte[] payloadBytes = gzipCompress(payload.toString().getBytes());
        byte[] header = getHeader(CLIENT_FULL_REQUEST, POS_SEQUENCE, JSON, GZIP, (byte) 0);
        byte[] payloadSize = intToBytes(payloadBytes.length);
        byte[] seqBytes = intToBytes(seq);

        byte[] fullClientRequest = new byte[header.length + seqBytes.length + payloadSize.length + payloadBytes.length];
        System.arraycopy(header, 0, fullClientRequest, 0, header.length);
        System.arraycopy(seqBytes, 0, fullClientRequest, header.length, seqBytes.length);
        System.arraycopy(payloadSize, 0, fullClientRequest, header.length + seqBytes.length, payloadSize.length);
        System.arraycopy(payloadBytes, 0, fullClientRequest, header.length + seqBytes.length + payloadSize.length, payloadBytes.length);

        webSocket.send(ByteString.of(fullClientRequest));
    }

    private void sendAudioSegment(WebSocket webSocket, byte[] buffer, boolean isLast, int seq) {
        byte messageTypeSpecificFlags = isLast ? NEG_WITH_SEQUENCE : POS_SEQUENCE;
        int finalSeq = isLast ? -seq : seq;

        byte[] header = getHeader(CLIENT_AUDIO_ONLY_REQUEST, messageTypeSpecificFlags, JSON, GZIP, (byte) 0);
        byte[] sequenceBytes = intToBytes(finalSeq);
        byte[] payloadBytes = gzipCompress(buffer, buffer.length);
        byte[] payloadSize = intToBytes(payloadBytes.length);

        byte[] audioRequest = new byte[header.length + sequenceBytes.length + payloadSize.length + payloadBytes.length];
        System.arraycopy(header, 0, audioRequest, 0, header.length);
        System.arraycopy(sequenceBytes, 0, audioRequest, header.length, sequenceBytes.length);
        System.arraycopy(payloadSize, 0, audioRequest, header.length + sequenceBytes.length, payloadSize.length);
        System.arraycopy(payloadBytes, 0, audioRequest, header.length + sequenceBytes.length + payloadSize.length, payloadBytes.length);

        webSocket.send(ByteString.of(audioRequest));
    }

    private AsrResponse parseResponse(byte[] res) {
        if (res == null || res.length == 0) {
            return new AsrResponse();
        }

        AsrResponse result = new AsrResponse();

        int headerSize = res[0] & 0x0f;
        int messageType = (res[1] >> 4) & 0x0f;
        int messageTypeSpecificFlags = res[1] & 0x0f;
        int serializationMethod = (res[2] >> 4) & 0x0f;
        int messageCompression = res[2] & 0x0f;

        byte[] payload = Arrays.copyOfRange(res, headerSize * 4, res.length);

        if ((messageTypeSpecificFlags & 0x01) != 0) {
            result.payloadSequence = bytesToInt(Arrays.copyOfRange(payload, 0, 4));
            payload = Arrays.copyOfRange(payload, 4, payload.length);
        }
        if ((messageTypeSpecificFlags & 0x02) != 0) {
            result.isLastPackage = true;
        }
        if ((messageTypeSpecificFlags & 0x04) != 0) {
            result.event = bytesToInt(Arrays.copyOfRange(payload, 0, 4));
            payload = Arrays.copyOfRange(payload, 4, payload.length);
        }

        switch (messageType) {
            case SERVER_FULL_RESPONSE:
                result.payloadSize = bytesToInt(Arrays.copyOfRange(payload, 0, 4));
                payload = Arrays.copyOfRange(payload, 4, payload.length);
                break;
            case SERVER_ERROR_RESPONSE:
                result.code = bytesToInt(Arrays.copyOfRange(payload, 0, 4));
                result.payloadSize = bytesToInt(Arrays.copyOfRange(payload, 4, 8));
                payload = Arrays.copyOfRange(payload, 8, payload.length);
                break;
            default:
                break;
        }

        if (payload.length == 0) {
            return result;
        }

        if (messageCompression == GZIP) {
            payload = gzipDecompress(payload);
        }

        if (serializationMethod == JSON && payload != null) {
            result.payloadMsg = new String(payload);
        }

        return result;
    }

    private byte[] getHeader(byte messageType, byte messageTypeSpecificFlags,
                             byte serialMethod, byte compressionType, byte reservedData) {
        final byte[] header = new byte[4];
        header[0] = (byte) ((PROTOCOL_VERSION << 4) | DEFAULT_HEADER_SIZE);
        header[1] = (byte) ((messageType << 4) | messageTypeSpecificFlags);
        header[2] = (byte) ((serialMethod << 4) | compressionType);
        header[3] = reservedData;
        return header;
    }

    private byte[] intToBytes(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    private int bytesToInt(byte[] src) {
        if (src == null || (src.length != 4)) {
            throw new IllegalArgumentException("Invalid byte array for int conversion");
        }
        return ((src[0] & 0xFF) << 24)
                | ((src[1] & 0xff) << 16)
                | ((src[2] & 0xff) << 8)
                | ((src[3] & 0xff));
    }

    private byte[] gzipCompress(byte[] src) {
        return gzipCompress(src, src.length);
    }

    private byte[] gzipCompress(byte[] src, int len) {
        if (src == null || len == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(src, 0, len);
        } catch (IOException e) {
            log.error("GZIP 压缩失败", e);
            return new byte[0];
        }
        return out.toByteArray();
    }

    private byte[] gzipDecompress(byte[] src) {
        if (src == null || src.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream ins = new ByteArrayInputStream(src);
        try (GZIPInputStream gzip = new GZIPInputStream(ins)) {
            byte[] buffer = new byte[256];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log.error("GZIP 解压失败", e);
            return null;
        }
        return out.toByteArray();
    }

    private static class SessionState {
        private WebSocket backendWebSocket;
        private int messageSequence = 0;
        private String businessSessionId;
        /** 前端 PCM 实际采样率，须与 AudioContext.sampleRate 一致（Whisper WAV 头）。 */
        private int pcmSampleRate = 16000;
        private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        // Whisper pseudo realtime state
        private final AtomicBoolean partialTranscribing = new AtomicBoolean(false);
        private volatile long lastPartialAtMs = 0;
        private volatile String lastPartialText = "";
    }

    private static class AsrResponse {
        private int code;
        private int event;
        private boolean isLastPackage;
        private int payloadSequence;
        private int payloadSize;
        private String payloadMsg;

        @Override
        public String toString() {
            return "AsrResponse{" +
                    "code=" + code +
                    ", event=" + event +
                    ", isLastPackage=" + isLastPackage +
                    ", payloadSequence=" + payloadSequence +
                    ", payloadSize=" + payloadSize +
                    ", payloadMsg='" + payloadMsg + '\'' +
                    '}';
        }
    }
}
