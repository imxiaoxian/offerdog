package com.hanserdev.interview.model.vo.interview.template;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InterviewTemplateDetailRspVO {

    /**
     * 模板ID
     */
    private UUID templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 分类（前端工程师、后端工程师、HR面试等）
     */
    private String category;

    /**
     * 难度级别（junior/mid/senior）
     */
    private String difficultyLevel;

    /**
     * 预计时长（分钟）
     */
    private Integer estimatedDurationMinutes;

    /**
     * AI面试官的系统提示词
     */
    private String systemPrompt;

    /**
     * 默认的面试配置
     */
    private JsonNode defaultConfig;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 关联的面试计划模板
     */
    private InterviewPlanTemplateRspVO planTemplate;
}
