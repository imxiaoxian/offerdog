package com.hanserdev.interview.service;

import com.hanserdev.interview.model.vo.question.QuestionRecommendationRspVO;

import java.util.UUID;

/**
 * 推荐题目服务接口
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 22:01
 */
public interface QuestionRecommendationService {

    /**
     * 根据面试报告生成推荐题目
     *
     * @param sessionId 面试会话ID
     * @return 推荐题目清单
     */
    QuestionRecommendationRspVO generateRecommendQuestions(UUID sessionId);
}
