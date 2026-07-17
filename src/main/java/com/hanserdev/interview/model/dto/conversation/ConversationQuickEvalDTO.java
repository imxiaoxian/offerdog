package com.hanserdev.interview.model.dto.conversation;

import lombok.Data;

import java.util.List;

@Data
public class ConversationQuickEvalDTO {

    /**
     * AI 快速评估指标
     */
    private Double relevanceScore;
    private Double clarityScore;
    private Double completenessScore;

    /**
     * AI 建议
     */
    private Boolean shouldFollowUp;
    private String suggestedDirection;

    /**
     * AI 识别出的潜在问题
     */
    private List<String> detectedIssues;
}
