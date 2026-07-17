package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.InterviewReportDO;
import com.hanserdev.interview.domain.dataobject.InterviewSessionDO;
import com.hanserdev.interview.model.vo.interview.ReportDetailRspVO;
import com.hanserdev.interview.model.vo.interview.SessionDetailRspVO;

/**
 * 面试相关对象转换器
 */
public class InterviewAssembler {

    /**
     * InterviewSessionDO -> SessionDetailRspVO
     */
    public static SessionDetailRspVO toSessionDetailVO(InterviewSessionDO session) {
        if (session == null) {
            return null;
        }

        SessionDetailRspVO vo = new SessionDetailRspVO();
        vo.setSessionId(session.getSessionId());
        vo.setUserId(session.getUserId());
        vo.setTemplateId(session.getTemplateId());
        vo.setPlanTemplateId(session.getPlanTemplateId());
        vo.setStatus(session.getStatus());
        vo.setInterviewPlan(session.getInterviewPlan());
        vo.setCurrentQuestionIndex(session.getCurrentQuestionIndex());
        vo.setQuestionsCompleted(session.getQuestionsCompleted());

        // 从计划中获取总问题数
        if (session.getInterviewPlan() != null && session.getInterviewPlan().has("total_questions")) {
            vo.setTotalQuestions(session.getInterviewPlan().get("total_questions").asInt());
        }

        vo.setTotalMessagesCount(session.getTotalMessagesCount());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setExpectedEndTime(session.getExpectedEndTime());
        vo.setDurationMinutes(session.getDurationMinutes());
        vo.setMetadata(session.getMetadata());
        vo.setCreatedAt(session.getCreatedAt());

        return vo;
    }

    /**
     * InterviewReportDO -> ReportDetailRspVO
     */
    public static ReportDetailRspVO toReportDetailVO(InterviewReportDO report) {
        if (report == null) {
            return null;
        }

        ReportDetailRspVO vo = new ReportDetailRspVO();
        vo.setReportId(report.getReportId());
        vo.setSessionId(report.getSessionId());
        vo.setOverallScore(report.getOverallScore());
        vo.setPassStatus(report.getPassStatus());
        vo.setConfidenceLevel(report.getConfidenceLevel());
        vo.setReportContent(report.getReportContent());
        vo.setReportText(report.getReportText());
        vo.setReportHtml(report.getReportHtml());
        vo.setAiModel(report.getAiModel());
        vo.setGenerationTokensUsed(report.getGenerationTokensUsed());
        vo.setGenerationDurationMs(report.getGenerationDurationMs());
        vo.setVersion(report.getVersion());
        vo.setReviewStatus(report.getReviewStatus());
        vo.setCreatedAt(report.getCreatedAt());

        return vo;
    }

    private InterviewAssembler() {
        // 工具类,禁止实例化
    }
}
