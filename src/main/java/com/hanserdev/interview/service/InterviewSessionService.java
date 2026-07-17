package com.hanserdev.interview.service;

import com.hanserdev.interview.model.vo.interview.ConversationMessageRspVO;
import com.hanserdev.interview.model.vo.interview.SessionDetailRspVO;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.UUID;

/**
 * 面试会话服务接口
 */
public interface InterviewSessionService {

    /**
     * 创建面试会话
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     * @return 会话详情
     */
    SessionDetailRspVO createSession(Long userId, UUID templateId);

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    SessionDetailRspVO getSessionDetail(UUID sessionId);

    /**
     * 结束面试会话
     *
     * @param sessionId 会话ID
     */
    void endSession(UUID sessionId);

    /**
     * 更新面试计划（由AI服务调用）
     *
     * @param sessionId 会话ID
     * @param planJson  更新后的计划JSON
     */
    void updateInterviewPlan(UUID sessionId, String planJson);

    /**
     * 获取指定用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<SessionDetailRspVO> getUserSessions(Long userId);

    /**
     * 获取指定用户所有会话的VO列表
     *
     * @param userId      用户ID
     * @param httpSession HTTP会话
     * @return 会话VO列表
     */
    List<SessionDetailRspVO> getSessionDetailRspVOS(Long userId, HttpSession httpSession);

    /**
     * 获取指定会话的全部对话消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ConversationMessageRspVO> getSessionMessages(UUID sessionId);
}
