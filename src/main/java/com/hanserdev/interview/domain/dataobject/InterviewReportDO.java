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
 * 面试报告实体类
 * AI生成的最终评估报告
 */
@Data
@TableName(value = "interview_reports", autoResultMap = true)
public class InterviewReportDO {

    /**
     * 报告ID，主键UUID
     */
    @TableId(value = "report_id")
    private UUID reportId;

    /**
     * 会话ID（唯一）
     */
    @TableField("session_id")
    private UUID sessionId;

    /**
     * 总体评分（0-10分，保留1位小数）
     */
    @TableField("overall_score")
    private BigDecimal overallScore;

    /**
     * 是否通过
     */
    @TableField("pass_status")
    private Boolean passStatus;

    /**
     * 信心水平（high/medium/low）- AI对评分的信心程度
     */
    @TableField("confidence_level")
    private String confidenceLevel;

    /**
     * 详细报告内容（JSONB格式）
     * 包含：summary、strengths、weaknesses、dimension_scores、question_analysis等
     */
    @TableField(value = "report_content", jdbcType = JdbcType.OTHER, typeHandler = JsonbTypeHandler.class)
    private JsonNode reportContent;

    /**
     * 纯文本版本报告
     */
    @TableField("report_text")
    private String reportText;

    /**
     * HTML版本报告
     */
    @TableField("report_html")
    private String reportHtml;

    /**
     * AI模型名称
     */
    @TableField("ai_model")
    private String aiModel;

    /**
     * 生成报告的提示词（可选，用于调试）
     */
    @TableField("generation_prompt")
    private String generationPrompt;

    /**
     * 生成报告使用的token数
     */
    @TableField("generation_tokens_used")
    private Integer generationTokensUsed;

    /**
     * 生成报告耗时（毫秒）
     */
    @TableField("generation_duration_ms")
    private Integer generationDurationMs;

    /**
     * 报告版本（如果重新生成会递增）
     */
    @TableField("version")
    private Integer version;

    /**
     * 审核状态（pending/approved/rejected/revised）
     */
    @TableField("review_status")
    private String reviewStatus;

    /**
     * 审核人ID
     */
    @TableField("reviewed_by")
    private UUID reviewedBy;

    /**
     * 审核时间
     */
    @TableField(value = "reviewed_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime reviewedAt;

    /**
     * 审核备注
     */
    @TableField("review_notes")
    private String reviewNotes;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", jdbcType = JdbcType.TIMESTAMP_WITH_TIMEZONE, typeHandler = PostgresTimestamptzTypeHandler.class)
    private LocalDateTime createdAt;
}

