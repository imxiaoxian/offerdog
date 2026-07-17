package com.hanserdev.interview.event;

import com.hanserdev.interview.enums.ReportGenerationStatus;
import lombok.Getter;

import java.util.UUID;

/**
 * 面试报告生成事件（用于通知WebSocket）
 */
@Getter
public class InterviewReportGeneratedEvent {

    private final UUID sessionId;
    private final Long userId;
    private final UUID reportId;
    private final ReportGenerationStatus status;
    private final String message;

    private InterviewReportGeneratedEvent(UUID sessionId, Long userId, UUID reportId,
                                          ReportGenerationStatus status, String message) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.reportId = reportId;
        this.status = status;
        this.message = message;
    }

    public static InterviewReportGeneratedEvent queued(UUID sessionId, Long userId, String message) {
        return new InterviewReportGeneratedEvent(sessionId, userId, null, ReportGenerationStatus.QUEUED, message);
    }

    public static InterviewReportGeneratedEvent processing(UUID sessionId, Long userId, String message) {
        return new InterviewReportGeneratedEvent(sessionId, userId, null, ReportGenerationStatus.PROCESSING, message);
    }

    public static InterviewReportGeneratedEvent success(UUID sessionId, Long userId, UUID reportId, String message) {
        return new InterviewReportGeneratedEvent(sessionId, userId, reportId, ReportGenerationStatus.SUCCESS, message);
    }

    public static InterviewReportGeneratedEvent failure(UUID sessionId, Long userId, String message) {
        return new InterviewReportGeneratedEvent(sessionId, userId, null, ReportGenerationStatus.FAILED, message);
    }
}
