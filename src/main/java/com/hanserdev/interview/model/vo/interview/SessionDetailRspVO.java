package com.hanserdev.interview.model.vo.interview;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 面试会话详情响应VO
 */
@Data
public class SessionDetailRspVO {

    /**
     * 会话ID
     */
    private UUID sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模板ID
     */
    private UUID templateId;

    /**
     * 计划模板ID
     */
    private UUID planTemplateId;

    /**
     * 会话状态
     */
    private String status;

    /**
     * 动态面试计划
     */
    private JsonNode interviewPlan;

    /**
     * 当前问题索引
     */
    private Integer currentQuestionIndex;

    /**
     * 已完成问题数
     */
    private Integer questionsCompleted;

    /**
     * 总问题数
     */
    private Integer totalQuestions;

    /**
     * 总消息数
     */
    private Integer totalMessagesCount;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 预计结束时间
     */
    private LocalDateTime expectedEndTime;

    /**
     * 实际时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 元数据
     */
    private JsonNode metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
