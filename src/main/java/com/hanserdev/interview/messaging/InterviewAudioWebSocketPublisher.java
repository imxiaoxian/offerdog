package com.hanserdev.interview.messaging;

import com.hanserdev.interview.model.vo.interview.AudioStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 将实时语音片段推送给订阅的前端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAudioWebSocketPublisher {

    private static final String DESTINATION_TEMPLATE = "/topic/interview/audio/%s";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(UUID sessionId, AudioStreamMessage payload) {
        String destination = String.format(DESTINATION_TEMPLATE, sessionId);
        log.debug("推送语音事件: dest={}, type={}, seq={}, chunk={}",
                destination, payload.getType(), payload.getSequenceNumber(), payload.getChunkIndex());
        messagingTemplate.convertAndSend(destination, payload);
    }
}
