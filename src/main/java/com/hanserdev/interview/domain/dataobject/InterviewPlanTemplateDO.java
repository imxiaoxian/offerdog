package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hanserdev.interview.domain.typehandler.JsonbTypeHandler;
import com.hanserdev.interview.domain.typehandler.PostgresTimestamptzTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 面试计划模板实体类
 * 预定义的面试问题流程和计划
 */
@Data
@TableName(value = "interview_plan_templates", autoResultMap = true)
public class InterviewPlanTemplateDO {

    /**
     * 计划模板ID，主键UUID
     */
    @TableId(value = "plan_template_id")
    private UUID planTemplateId;

    /**
     * 关联的面试模板ID
     */
    @TableField("template_id")
    private UUID templateId;

    /**
     * 计划名称
     */
    @TableField("plan_name")
    private String planName;

    /**
     * 预定义的面试计划结构（JSONB格式）
     * 包含：questions数组、transition_rules等
     */
    @TableField(value = "plan_structure", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private JsonNode planStructure;

    /**
     * 使用次数统计
     */
    @TableField("usage_count")
    private Integer usageCount;

    /**
     * 平均完成率
     */
    @TableField("avg_completion_rate")
    private BigDecimal avgCompletionRate;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

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

