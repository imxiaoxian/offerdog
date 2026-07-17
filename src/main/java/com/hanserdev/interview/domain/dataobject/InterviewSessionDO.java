package com.hanserdev.interview.domain.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * 面试会话实体类（核心表）
 * 记录每次面试的主信息和动态计划
 */
@Data
@TableName(value = "interview_sessions", autoResultMap = true)
public class InterviewSessionDO {

    /**
     * 会话ID，主键UUID
     */
    @TableId(value = "session_id")
    private UUID sessionId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 模板ID
     */
    @TableField("template_id")
    private UUID templateId;

    /**
     * 计划模板ID
     */
    @TableField("plan_template_id")
    private UUID planTemplateId;

    /**
     * 会话状态（in_progress/completed/abandoned/expired）
     */
    @TableField("status")
    private String status;

    /**
     * 动态面试计划（JSONB格式）
     * 会在面试过程中实时更新
     */
    @TableField(value = "interview_plan", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private JsonNode interviewPlan;

    /**
     * 当前问题索引
     */
    @TableField("current_question_index")
    private Integer currentQuestionIndex;

    /**
     * 已完成的问题数
     */
    @TableField("questions_completed")
    private Integer questionsCompleted;

    /**
     * 总消息数
     */
    @TableField("total_messages_count")
    private Integer totalMessagesCount;

    /**
     * 开始时间
     */
    @TableField(value = "start_time", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime endTime;

    /**
     * 预计结束时间
     */
    @TableField(value = "expected_end_time", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime expectedEndTime;

    /**
     * 实际面试时长（分钟，计算字段，由数据库生成）
     */
    @TableField(value = "duration_minutes", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer durationMinutes;

    /**
     * 元数据（JSONB格式）
     * 可存储：简历摘要、用户设备信息、中断次数等
     */
    @TableField(value = "metadata", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private JsonNode metadata;

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

