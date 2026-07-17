package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 面试模板响应VO
 */
@Data
public class InterviewTemplateRspVO {

    /**
     * 模板ID
     */
    private UUID id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 难度级别（junior/mid/senior）
     */
    private String difficultyLevel;

    /**
     * 预计时长（分钟）
     */
    private Integer estimatedDurationMinutes;

    /**
     * 分类（前端工程师、后端工程师、HR面试等）
     */
    private String category;

    /**
     * AI面试官的系统提示词
     */
    private String systemPrompt;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
