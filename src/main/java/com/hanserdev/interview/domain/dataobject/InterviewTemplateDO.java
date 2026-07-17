package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 面试模板实体类
 * 定义不同类型和难度的面试（前端、后端、全栈等）
 */
@Data
@TableName(value = "interview_templates", autoResultMap = true)
public class InterviewTemplateDO {

    /**
     * 模板ID，主键UUID
     */
    @TableId(value = "template_id")
    private UUID templateId;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 模板描述
     */
    @TableField("description")
    private String description;

    /**
     * 分类（前端工程师、后端工程师、HR面试等）
     */
    @TableField("category")
    private String category;

    /**
     * 难度级别（junior/mid/senior）
     */
    @TableField("difficulty_level")
    private String difficultyLevel;

    /**
     * 预计时长（分钟）
     */
    @TableField("estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    /**
     * AI面试官的系统提示词模板
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 默认的面试配置
     */
    @TableField(value = "default_config", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private JsonNode defaultConfig;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;
}

