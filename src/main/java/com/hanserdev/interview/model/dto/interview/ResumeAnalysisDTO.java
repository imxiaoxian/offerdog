package com.hanserdev.interview.model.dto.interview;

import lombok.Data;

import java.util.List;

/**
 * 简历分析结果DTO
 * AI分析简历后提取的关键信息
 */
@Data
public class ResumeAnalysisDTO {

    /**
     * 工作年限
     */
    private Integer workYears;

    /**
     * 当前技术水平（junior/mid/senior/high_senior）
     */
    private String currentLevel;

    /**
     * 技术栈列表
     */
    private List<String> technicalStack;

    /**
     * 强项领域
     */
    private List<String> strongAreas;

    /**
     * 弱项领域
     */
    private List<String> weakAreas;

    /**
     * 面试重点关注点
     */
    private List<String> interviewFocusPoints;

    /**
     * AI建议的问题列表
     */
    private List<String> suggestedQuestions;
}

