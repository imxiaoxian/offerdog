package com.hanserdev.interview.service;

import com.hanserdev.interview.config.InterviewAsrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperAsrService {

    private final InterviewAsrProperties asrProperties;

    @Qualifier("whisperWebClient")
    private final WebClient webClient;

    public Mono<Map> transcribe(byte[] audioData, String language) {
        String whisperUrl = asrProperties.getWhisperUrl();
        String endpoint = whisperUrl + "/transcribe";
        
        log.info("[Whisper] Calling Whisper service at {}", endpoint);

        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        Map<String, Object> requestBody = buildWhisperRequestBody(base64Audio, language);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> log.info("[Whisper] Transcription success: {}", response))
                .doOnError(error -> log.error("[Whisper] Transcription error: {}", error.getMessage()));
    }

    public String transcribeSync(byte[] audioData, String language) {
        String whisperUrl = asrProperties.getWhisperUrl();
        String endpoint = whisperUrl + "/transcribe";
        
        log.info("[Whisper] Calling Whisper service at {} (sync)", endpoint);

        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        Map<String, Object> requestBody = buildWhisperRequestBody(base64Audio, language);

        try {
            Map response = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                if (response.get("error") != null) {
                    throw new RuntimeException(String.valueOf(response.get("error")));
                }
                if (response.get("text") != null) {
                    return (String) response.get("text");
                }
            }
            return "";
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("[Whisper] HTTP {} from {}: {}", e.getStatusCode().value(), endpoint, body);
            throw new RuntimeException("Whisper HTTP " + e.getStatusCode().value() + ": " + body, e);
        } catch (Exception e) {
            log.error("[Whisper] Transcription error: {}", e.getMessage());
            throw new RuntimeException("Whisper transcription failed: " + e.getMessage() + connectionHint(e), e);
        }
    }

    private static String connectionHint(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.ConnectException) {
                return "（无法连接 Whisper 服务，请确认已启动 whisper-service 或 docker compose up whisper，且 WHISPER_ENDPOINT 与映射端口一致，如 http://127.0.0.1:5000）";
            }
        }
        return "";
    }

    private static Map<String, Object> buildWhisperRequestBody(String base64Audio, String language) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("audio", base64Audio);
        if (StringUtils.isNotBlank(language) && !"auto".equalsIgnoreCase(language.trim())) {
            body.put("language", language.trim());
        }
        return body;
    }
}
