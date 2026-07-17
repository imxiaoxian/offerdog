package com.hanserdev.interview.model.vo.interview;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 面试报告详情响应VO
 */
@Data
public class ReportDetailRspVO {

    /**
     * 报告ID
     */
    private UUID reportId;

    /**
     * 会话ID
     */
    private UUID sessionId;

    /**
     * 总体评分
     */
    private BigDecimal overallScore;

    /**
     * 是否通过
     */
    private Boolean passStatus;

    /**
     * 信心水平
     */
    private String confidenceLevel;

    /**
     * 详细报告内容
     */
    private JsonNode reportContent;

    /**
     * 纯文本报告
     */
    private String reportText;

    /**
     * HTML报告
     */
    private String reportHtml;

    /**
     * AI模型
     */
    private String aiModel;

    /**
     * token使用量
     */
    private Integer generationTokensUsed;

    /**
     * 生成耗时（毫秒）
     */
    private Integer generationDurationMs;

    /**
     * 报告版本
     */
    private Integer version;

    /**
     * 审核状态
     */
    private String reviewStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

