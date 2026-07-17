package com.hanserdev.interview.messaging;

import com.hanserdev.interview.event.InterviewReportGeneratedEvent;
import com.hanserdev.interview.model.vo.interview.ReportGenerationStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 将报告生成事件推送给订阅的前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReportWebSocketNotifier {

    private static final String DESTINATION_TEMPLATE = "/topic/interview/report/%s";

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleReportGeneratedEvent(InterviewReportGeneratedEvent event) {
        ReportGenerationStatusMessage payload = ReportGenerationStatusMessage.builder()
                .sessionId(event.getSessionId())
                .reportId(event.getReportId())
                .userId(event.getUserId())
                .status(event.getStatus())
                .message(event.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        String destination = String.format(DESTINATION_TEMPLATE, event.getSessionId());
        log.info("报告生成事件广播：dest={}, status={}, sessionId={}",
                destination, payload.getStatus(), event.getSessionId());
        messagingTemplate.convertAndSend(destination, payload);
    }
}
