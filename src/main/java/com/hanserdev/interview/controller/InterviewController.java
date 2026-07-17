package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.*;
import com.hanserdev.interview.model.vo.question.QuestionRecommendationRspVO;
import com.hanserdev.interview.service.InterviewAIService;
import com.hanserdev.interview.service.InterviewLearningPackService;
import com.hanserdev.interview.service.InterviewReportService;
import com.hanserdev.interview.service.InterviewSessionService;
import com.hanserdev.interview.service.QuestionRecommendationService;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.utils.UserSessionUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 面试控制器
 */
@Slf4j
@RestController
@RequestMapping("/interview")
public class InterviewController {

    @Resource
    private InterviewSessionService sessionService;

    @Resource
    private InterviewAIService aiService;

    @Resource
    private InterviewReportService reportService;

    @Resource
    private QuestionRecommendationService recommendationService;

    @Resource
    private InterviewLearningPackService learningPackService;

    /**
     * 获取指定用户的所有面试会话
     */
    @GetMapping("/users/{userId}/sessions")
    @ApiOperationLog(description = "获取用户面试会话列表")
    public Response<List<SessionDetailRspVO>> getUserSessions(
            @PathVariable Long userId,
            HttpSession httpSession) {
        List<SessionDetailRspVO> sessions = sessionService.getSessionDetailRspVOS(userId, httpSession);
        return Response.success(sessions);
    }

    /**
     * 已生成报告的历史得分序列（按报告时间升序），用于能力成长曲线。
     */
    @GetMapping("/users/{userId}/growth-points")
    @ApiOperationLog(description = "获取用户面试成长曲线数据")
    public Response<List<InterviewGrowthPointRspVO>> getGrowthPoints(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit,
            HttpSession httpSession) {
        UserSessionDTO userSession = UserSessionUtils.getRequiredUser(httpSession);
        if (!userSession.getUserId().equals(userId)) {
            throw new ApiException(ResponseCodeEnum.USER_ROLE_INVALID);
        }
        return Response.success(reportService.listGrowthPoints(userId, limit));
    }

    /**
     * 获取面试模板列表
     */
    @GetMapping("/templates")
    @ApiOperationLog(description = "获取面试模板列表")
    public Response<List<InterviewTemplateRspVO>> getTemplates() {
        List<InterviewTemplateRspVO> result = aiService.getInterviewTemplateRspVOS();

        return Response.success(result);
    }

    /**
     * 创建面试会话
     */
    @PostMapping("/sessions")
    @ApiOperationLog(description = "创建面试会话")
    public Response<SessionDetailRspVO> createSession(
            @Valid @RequestBody CreateSessionReqVO reqVO,
            HttpSession httpSession) {
        // 从session中获取用户ID
        UserSessionDTO userSession = UserSessionUtils.getRequiredUser(httpSession);
        Long userId = userSession.getUserId();

        SessionDetailRspVO session = sessionService.createSession(userId, reqVO.getTemplateId());
        return Response.success(session);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    @ApiOperationLog(description = "获取面试会话详情")
    public Response<SessionDetailRspVO> getSession(@PathVariable UUID sessionId) {
        SessionDetailRspVO session = sessionService.getSessionDetail(sessionId);
        return Response.success(session);
    }

    /**
     * 获取会话的对话消息
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @ApiOperationLog(description = "获取面试会话消息记录")
    public Response<List<ConversationMessageRspVO>> getSessionMessages(@PathVariable UUID sessionId) {
        List<ConversationMessageRspVO> messages = sessionService.getSessionMessages(sessionId);
        return Response.success(messages);
    }

    /**
     * 获取AI开场白
     */
    @GetMapping("/sessions/{sessionId}/opening")
    @ApiOperationLog(description = "获取面试会话开场白")
    public Response<String> getOpening(@PathVariable UUID sessionId) {
        String opening = aiService.generateOpening(sessionId);
        return Response.success(opening);
    }

    /**
     * 提交回答并获取AI响应
     */
    @PostMapping("/sessions/{sessionId}/answer")
    @ApiOperationLog(description = "提交面试会话回答并获取AI响应")
    public Response<AIResponseRspVO> submitAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerReqVO reqVO) {

        AIResponseRspVO response = aiService.generateResponse(
                sessionId,
                reqVO.getQuestionId(),
                reqVO.getAnswer()
        );
        return Response.success(response);
    }

    /**
     * 结束面试会话
     */
    @PostMapping("/sessions/{sessionId}/end")
    @ApiOperationLog(description = "结束面试会话")
    public Response<Void> endSession(@PathVariable UUID sessionId) {
        sessionService.endSession(sessionId);
        return Response.success();
    }

    /**
     * 异步生成面试报告
     */
    @PostMapping("/sessions/{sessionId}/report/generate")
    @ApiOperationLog(description = "异步生成面试报告")
    public Response<Void> generateReport(@PathVariable UUID sessionId) {
        reportService.generateReport(sessionId);
        Response<Void> response = Response.success();
        response.setMessage("报告生成任务已提交，完成后会通过WebSocket通知。");
        return response;
    }

    /**
     * 获取面试报告
     */
    @GetMapping("/sessions/{sessionId}/report")
    @ApiOperationLog(description = "获取面试报告")
    public Response<ReportDetailRspVO> getReport(@PathVariable UUID sessionId) {
        ReportDetailRspVO report = reportService.getReportBySessionId(sessionId);
        return Response.success(report);
    }

    /**
     * 获取基于面试报告的题目推荐
     */
    @GetMapping("/sessions/{sessionId}/recommendations")
    @ApiOperationLog(description = "获取面试报告题目推荐")
    public Response<QuestionRecommendationRspVO> getQuestionRecommendations(
            @PathVariable UUID sessionId) {
        QuestionRecommendationRspVO recommendations = recommendationService.generateRecommendQuestions(sessionId);
        return Response.success(recommendations);
    }

    /**
     * 智能学习包：知识库片段、精选外链、7 日个性化练习计划、推荐面试题（依赖已生成报告）。
     */
    @GetMapping("/sessions/{sessionId}/learning-pack")
    @ApiOperationLog(description = "获取智能学习资源包")
    public Response<InterviewLearningPackRspVO> getLearningPack(@PathVariable UUID sessionId) {
        return Response.success(learningPackService.buildLearningPack(sessionId));
    }
}
