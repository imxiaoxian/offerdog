package com.hanserdev.interview.model.vo.question;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 推荐题目
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 23:12
 */
@Data
@Builder
public class RecommendedQuestionVO {
    /**
     * 题目ID
     */
    private Long id;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 难度
     */
    private String difficulty;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 推荐原因（匹配的薄弱点）
     */
    private String matchedPoint;

    /**
     * 相似度分数
     */
    private Double similarityScore;
}
