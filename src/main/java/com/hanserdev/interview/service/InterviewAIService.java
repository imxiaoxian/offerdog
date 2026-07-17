package com.hanserdev.interview.service;

import com.hanserdev.interview.model.dto.interview.InterviewActionDTO;
import com.hanserdev.interview.model.vo.interview.AIResponseRspVO;
import com.hanserdev.interview.model.vo.interview.InterviewTemplateRspVO;

import java.util.List;
import java.util.UUID;

/**
 * AI面试服务接口
 */
public interface InterviewAIService {

    /**
     * 生成个性化开场白
     *
     * @param sessionId 会话ID
     * @return 开场白内容
     */
    String generateOpening(UUID sessionId);

    /**
     * 生成AI响应（根据用户回答）
     *
     * @param sessionId  会话ID
     * @param questionId 问题ID
     * @param answer     用户回答
     * @return AI响应
     */
    AIResponseRspVO generateResponse(UUID sessionId, String questionId, String answer);

    /**
     * 更新面试计划状态（根据AI动作）
     *
     * @param sessionId 会话ID
     * @param action    AI动作
     */
    void updateInterviewPlan(UUID sessionId, InterviewActionDTO action);

    /**
     * 获取面试模板列表
     *
     * @return 面试模板列表
     */
    List<InterviewTemplateRspVO> getInterviewTemplateRspVOS();
}

