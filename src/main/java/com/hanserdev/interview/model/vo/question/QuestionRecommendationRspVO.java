package com.hanserdev.interview.model.vo.question;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 推荐题目清单响应对象
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 22:02
 */
@Data
@Builder
public class QuestionRecommendationRspVO {

    /**
     * 会话ID
     */
    private UUID sessionId;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 薄弱知识点
     */
    private List<String> weakPoints;

    /**
     * 推荐题目列表
     */
    private List<RecommendedQuestionVO> recommendedQuestions;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
}
