package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.hanserdev.interview.constants.AIPromptConstants;
import com.hanserdev.interview.domain.dataobject.*;
import com.hanserdev.interview.domain.mapper.*;
import com.hanserdev.interview.enums.ConversationRoleEnum;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.event.InterviewReportGeneratedEvent;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.model.vo.interview.InterviewGrowthPointRspVO;
import com.hanserdev.interview.model.vo.interview.ReportDetailRspVO;
import com.hanserdev.interview.service.InterviewReportService;
import com.hanserdev.interview.service.support.ReportGenerationCoordinator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 面试报告服务实现
 */
@Slf4j
@Service
public class InterviewReportServiceImpl implements InterviewReportService {

    @Resource
    private InterviewReportMapper reportMapper;

    @Resource
    private InterviewSessionMapper sessionMapper;

    @Resource
    private InterviewTemplateMapper templateMapper;

    @Resource
    private ConversationMessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ChatClient dashScopeChatClient;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private ReportGenerationCoordinator reportGenerationCoordinator;

    @Resource
    private CacheManager cacheManager;

    @Override
    @Async("TaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void generateReport(UUID sessionId) {
        long startTime = System.currentTimeMillis();
        log.info("开始生成面试报告: sessionId={}", sessionId);

        Long userId = null;
        boolean taskRegistered = false;

        try {
            // 1. 获取完整会话数据
            InterviewSessionDO session = getSessionById(sessionId);
            userId = session.getUserId();
            final Long finalUserId = userId;

            InterviewReportDO latestReport = findLatestReport(sessionId);
            if (latestReport != null) {
                log.info("面试报告已存在，无需重新生成: sessionId={}", sessionId);
                eventPublisher.publishEvent(InterviewReportGeneratedEvent.success(
                        sessionId,
                        finalUserId,
                        latestReport.getReportId(),
                        "报告已存在，返回最新版本"));
                return;
            }

            if (!reportGenerationCoordinator.tryMarkQueued(sessionId)) {
                log.warn("面试报告生成任务已在执行: sessionId={}", sessionId);
                eventPublisher.publishEvent(InterviewReportGeneratedEvent.processing(
                        sessionId,
                        finalUserId,
                        "报告生成中，请勿重复提交"));
                return;
            }
            taskRegistered = true;

            eventPublisher.publishEvent(InterviewReportGeneratedEvent.queued(
                    sessionId,
                    finalUserId,
                    "报告生成任务已进入队列"));

            reportGenerationCoordinator.markProcessing(sessionId);
            eventPublisher.publishEvent(InterviewReportGeneratedEvent.processing(
                    sessionId,
                    finalUserId,
                    "报告生成中..."));

            InterviewTemplateDO template = getTemplateById(session.getTemplateId());

            // 2. 获取对话消息
            List<ConversationMessageDO> messages = getMessages(sessionId);

            // 3. 获取用户简历
            UserDO user = getUserById(session.getUserId());
            UserResumeDTO resume = user.getResume();

            // 4. 构建报告生成提示词
            String reportPrompt = buildReportPrompt(session, template, messages, resume);

            // 5. 调用AI生成报告
            String jsonResponse = dashScopeChatClient.prompt(reportPrompt).call().content();

            // 6. 解析JSON报告
            jsonResponse = cleanJsonResponse(jsonResponse);
            JsonNode reportContent = objectMapper.readTree(jsonResponse);

            // 7. 生成文本和HTML版本
            String textVersion = generateTextReport(reportContent);
            String htmlVersion = generateHtmlReport(reportContent);

            // 8. 保存报告
            InterviewReportDO report = new InterviewReportDO();
            report.setReportId(UUID.randomUUID());
            report.setSessionId(sessionId);
            report.setOverallScore(new BigDecimal(reportContent.path("overall_score").asText("0")));
            report.setPassStatus(reportContent.path("pass_status").asBoolean(false));
            report.setConfidenceLevel(reportContent.path("confidence_level").asText("unknown"));
            report.setReportContent(reportContent);
            report.setReportText(textVersion);
            report.setReportHtml(htmlVersion);
            report.setAiModel("qwen-plus");
            report.setGenerationTokensUsed(estimateTokens(jsonResponse));
            report.setGenerationDurationMs((int) (System.currentTimeMillis() - startTime));
            report.setVersion(1);
            report.setReviewStatus("pending");

            reportMapper.insert(report);

            log.info("面试报告生成成功: sessionId={}, score={}", sessionId, report.getOverallScore());
            publishEventAfterCommit(() -> {
                evictInterviewSessionCaches(sessionId);
                eventPublisher.publishEvent(InterviewReportGeneratedEvent.success(
                        sessionId,
                        finalUserId,
                        report.getReportId(),
                        "报告生成完成"));
            });
        } catch (Exception e) {
            log.error("生成报告失败: sessionId={}", sessionId, e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "系统异常，请稍后重试";
            eventPublisher.publishEvent(InterviewReportGeneratedEvent.failure(sessionId, userId, errorMessage));
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        } finally {
            if (taskRegistered) {
                reportGenerationCoordinator.clear(sessionId);
            }
        }
    }

    private void publishEventAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void evictInterviewSessionCaches(UUID sessionId) {
        String key = sessionId.toString();
        Cache reports = cacheManager.getCache("interviewReports");
        if (reports != null) {
            reports.evict(key);
        }
        Cache packs = cacheManager.getCache("interviewLearningPacks");
        if (packs != null) {
            packs.evict(key);
        }
    }

    @Override
    @Cacheable(cacheNames = "interviewReports", key = "#sessionId.toString()")
    public ReportDetailRspVO getReportBySessionId(UUID sessionId) {
        InterviewReportDO report = findLatestReport(sessionId);
        if (report == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_REPORT_NOT_FOUND);
        }

        return convertToVO(report);
    }

    @Override
    public List<InterviewGrowthPointRspVO> listGrowthPoints(Long userId, int limit) {
        int cap = Math.min(Math.max(limit, 1), 50);
        LambdaQueryWrapper<InterviewSessionDO> sw = new LambdaQueryWrapper<>();
        sw.eq(InterviewSessionDO::getUserId, userId)
                .eq(InterviewSessionDO::getStatus, "completed")
                .orderByDesc(InterviewSessionDO::getEndTime)
                .last("LIMIT " + (cap * 3));

        List<InterviewSessionDO> sessions = sessionMapper.selectList(sw);
        List<InterviewGrowthPointRspVO> raw = new ArrayList<>();
        for (InterviewSessionDO s : sessions) {
            if (raw.size() >= cap) {
                break;
            }
            InterviewReportDO report = findLatestReport(s.getSessionId());
            if (report == null || report.getReportContent() == null) {
                continue;
            }
            JsonNode dim = report.getReportContent().path("dimension_scores");
            raw.add(InterviewGrowthPointRspVO.builder()
                    .sessionId(s.getSessionId())
                    .reportCreatedAt(report.getCreatedAt())
                    .overallScore(report.getOverallScore())
                    .technicalKnowledge(readScore(dim, "technical_knowledge"))
                    .communication(readScore(dim, "communication"))
                    .problemSolving(readScore(dim, "problem_solving"))
                    .systemDesign(readScore(dim, "system_design"))
                    .practicalExperience(readScore(dim, "practical_experience"))
                    .learningAbility(readScore(dim, "learning_ability"))
                    .build());
        }
        raw.sort(Comparator.comparing(InterviewGrowthPointRspVO::getReportCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return raw;
    }

    private static Double readScore(JsonNode dim, String field) {
        if (dim == null || dim.isMissingNode() || !dim.has(field)) {
            return null;
        }
        double v = dim.get(field).asDouble();
        return Double.isFinite(v) ? v : null;
    }

    /**
     * 构建报告生成提示词
     */
    private String buildReportPrompt(InterviewSessionDO session, InterviewTemplateDO template,
                                     List<ConversationMessageDO> messages, UserResumeDTO resume) {
        JsonNode plan = session.getInterviewPlan();
        if (plan == null || plan.isNull() || plan.isMissingNode()) {
            plan = MissingNode.getInstance();
        }
        JsonNode metadata = session.getMetadata();
        if (metadata == null || metadata.isNull() || metadata.isMissingNode()) {
            metadata = MissingNode.getInstance();
        }

        // 格式化对话历史
        String conversationHistory = messages.stream()
                .map(msg -> String.format("[%s]: %s",
                        msg.getRole() == ConversationRoleEnum.INTERVIEWER ? "面试官" : "候选人",
                        msg.getContent() != null ? msg.getContent() : ""))
                .collect(Collectors.joining("\n\n"));

        // 格式化问题列表
        StringBuilder questionsInfo = new StringBuilder();
        JsonNode questionsNode = plan.path("questions");
        if (questionsNode.isArray()) {
            ArrayNode questions = (ArrayNode) questionsNode;
            for (int i = 0; i < questions.size(); i++) {
                JsonNode q = questions.get(i);
                questionsInfo.append(String.format("""
                        问题%d:
                        - 主题: %s
                        - 问题: %s
                        - 难度: %s
                        - 追问次数: %d
                        - 状态: %s
                        
                        """,
                        i + 1,
                        q.path("topic").asText(""),
                        q.path("initial_question").asText(""),
                        q.has("difficulty") ? q.get("difficulty").asText() : "mid",
                        q.path("follow_up_count").asInt(0),
                        q.path("status").asText("")
                ));
            }
        } else {
            questionsInfo.append("（会话中未保存结构化问题列表，请仅依据对话记录评估。）\n");
        }

        // 格式化简历信息
        String resumeInfo = buildResumeInfo(resume, metadata);

        // 计算实际时长
        int durationMinutes = session.getDurationMinutes() != null ? session.getDurationMinutes() : 0;

        int totalQuestions = plan.path("total_questions").asInt(0);
        if (totalQuestions <= 0 && session.getQuestionsCompleted() != null) {
            totalQuestions = Math.max(session.getQuestionsCompleted(), 1);
        }

        String resumeLevel = metadata.path("resume_level").asText("mid");
        int workYears = metadata.path("work_years").asInt(0);

        String expressionSignals = buildExpressionSignals(messages, durationMinutes);

        return String.format(
                AIPromptConstants.REPORT_GENERATION_PROMPT_TEMPLATE,
                template.getTemplateName(),
                template.getDifficultyLevel(),
                durationMinutes,
                totalQuestions,
                session.getQuestionsCompleted() != null ? session.getQuestionsCompleted() : 0,
                resumeLevel,
                workYears,
                resumeInfo,
                expressionSignals,
                conversationHistory,
                questionsInfo.toString()
        );
    }

    /**
     * 基于文本消息的粗统计，供大模型推断表达维度；非真实语速/声纹测量。
     */
    private static String buildExpressionSignals(List<ConversationMessageDO> messages, int durationMinutes) {
        List<ConversationMessageDO> candidate = messages.stream()
                .filter(m -> m.getRole() == ConversationRoleEnum.CANDIDATE)
                .toList();
        if (candidate.isEmpty()) {
            return "（无候选人消息）";
        }
        int n = candidate.size();
        int totalChars = candidate.stream()
                .mapToInt(m -> m.getContent() == null ? 0 : m.getContent().length())
                .sum();
        int avg = n > 0 ? totalChars / n : 0;
        int maxLen = candidate.stream()
                .mapToInt(m -> m.getContent() == null ? 0 : m.getContent().length())
                .max()
                .orElse(0);
        StringBuilder sb = new StringBuilder();
        sb.append("- 候选人发言条数: ").append(n).append("\n");
        sb.append("- 候选人发言总字符数（近似）: ").append(totalChars).append("\n");
        sb.append("- 平均每条约 ").append(avg).append(" 字符，最长一条约 ").append(maxLen).append(" 字符\n");
        if (durationMinutes > 0) {
            sb.append("- 面试时长约 ").append(durationMinutes).append(" 分钟\n");
            sb.append("- 粗略信息密度: 约 ")
                    .append(String.format(java.util.Locale.ROOT, "%.1f", totalChars / (double) durationMinutes))
                    .append(" 字符/分钟（非真实语速，仅作参考）\n");
        }
        sb.append("- 说明: 语音作答经识别后与打字共用文本内容；若无音频特征，请勿声称完成声学或情感模型推理。");
        return sb.toString();
    }

    /**
     * 构建简历信息
     */
    private String buildResumeInfo(UserResumeDTO resume, JsonNode metadata) {
        if (resume == null) {
            return "无简历信息";
        }
        if (metadata == null || metadata.isNull() || metadata.isMissingNode()) {
            metadata = MissingNode.getInstance();
        }

        // 格式化工作经历
        String workExp = "无";
        if (resume.getWork() != null && resume.getWork().getExperience() != null) {
            workExp = resume.getWork().getExperience().stream()
                    .map(exp -> String.format("- %s @ %s (%s - %s)",
                            exp.getTitle(), exp.getCompany(),
                            exp.getStartDate(), exp.getEndDate() != null ? exp.getEndDate() : "至今"))
                    .collect(Collectors.joining("\n"));
        }

        // 格式化项目经历
        String projectExp = "无";
        if (resume.getWork() != null && resume.getWork().getProjects() != null) {
            projectExp = resume.getWork().getProjects().stream()
                    .map(proj -> String.format("- %s (角色: %s)", proj.getName(), proj.getRole()))
                    .collect(Collectors.joining("\n"));
        }

        return String.format(
                AIPromptConstants.RESUME_INFO_TEMPLATE,
                resume.getName(),
                metadata.path("work_years").asInt(0),
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getLevel() : "未知",
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getSchool() : "未知",
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getMajor() : "未知",
                resume.getJobIntention() != null ? resume.getJobIntention() : "未知",
                workExp,
                projectExp,
                resume.getSkills() != null ? String.join(", ", resume.getSkills()) : "无",
                resume.getSelfEvaluation() != null ? resume.getSelfEvaluation() : "无"
        );
    }

    /**
     * 生成纯文本报告
     */
    private String generateTextReport(JsonNode reportContent) {
        StringBuilder text = new StringBuilder();

        text.append("========================================\n");
        text.append("           面试评估报告\n");
        text.append("========================================\n\n");

        text.append(String.format("总体评分: %.1f/10\n",
                reportContent.path("overall_score").asDouble(0)));
        text.append(String.format("评估结果: %s\n",
                reportContent.path("pass_status").asBoolean(false) ? "通过" : "未通过"));
        text.append(String.format("评分信心: %s\n\n",
                reportContent.path("confidence_level").asText("unknown")));

        text.append("## 总体评价\n");
        text.append(reportContent.path("summary").asText("(无)")).append("\n\n");

        text.append("## 优势\n");
        appendJsonArrayLines(text, reportContent.path("strengths"));
        text.append("\n");

        text.append("## 待改进方面\n");
        appendJsonArrayLines(text, reportContent.path("weaknesses"));
        text.append("\n");

        text.append("## 各维度评分\n");
        JsonNode dimensions = reportContent.get("dimension_scores");
        if (dimensions != null && !dimensions.isMissingNode()) {
            dimensions.fieldNames().forEachRemaining(fieldName -> {
                JsonNode value = dimensions.get(fieldName);
                text.append(String.format("- %s: %.1f/10\n",
                        fieldName, value.asDouble()));
            });
        }
        text.append("\n");

        JsonNode contentAnalysis = reportContent.path("content_analysis");
        if (contentAnalysis != null && !contentAnalysis.isMissingNode() && contentAnalysis.isObject()) {
            text.append("## 内容分析（技术正确性、深度、逻辑、岗位匹配）\n");
            text.append(String.format("- technical_correctness: %.1f\n", contentAnalysis.path("technical_correctness").asDouble(0)));
            text.append(String.format("- knowledge_depth: %.1f\n", contentAnalysis.path("knowledge_depth").asDouble(0)));
            text.append(String.format("- logical_rigor: %.1f\n", contentAnalysis.path("logical_rigor").asDouble(0)));
            text.append(String.format("- job_fit: %.1f\n", contentAnalysis.path("job_fit").asDouble(0)));
            text.append("亮点: ").append(contentAnalysis.path("highlights").asText("(无)")).append("\n");
            text.append("不足: ").append(contentAnalysis.path("gaps").asText("(无)")).append("\n\n");
        }

        JsonNode expr = reportContent.path("expression_analysis");
        if (expr != null && !expr.isMissingNode() && expr.isObject()) {
            text.append("## 表达分析（基于文本与统计推断）\n");
            text.append(String.format("- pace_score: %.1f\n", expr.path("pace_score").asDouble(0)));
            text.append(String.format("- clarity_score: %.1f\n", expr.path("clarity_score").asDouble(0)));
            text.append(String.format("- confidence_score: %.1f\n", expr.path("confidence_score").asDouble(0)));
            text.append(String.format("- emotional_stability_score: %.1f\n", expr.path("emotional_stability_score").asDouble(0)));
            text.append("依据: ").append(expr.path("evidence_from_text").asText("(无)")).append("\n");
            text.append("说明: ").append(expr.path("caveat").asText("(无)")).append("\n\n");
        }

        JsonNode plan = reportContent.path("practice_plan");
        if (plan != null && !plan.isMissingNode() && plan.isObject()) {
            text.append("## 能力提升练习计划\n");
            text.append(plan.path("focus_summary").asText("(无)")).append("\n");
            text.append("周安排:\n");
            appendJsonArrayLines(text, plan.path("weekly_schedule"));
            text.append("建议练习:\n");
            appendJsonArrayLines(text, plan.path("drills"));
            text.append("\n");
        }

        text.append("## 发展建议\n");
        appendJsonArrayLines(text, reportContent.path("recommendations"));

        return text.toString();
    }

    private static void appendJsonArrayLines(StringBuilder text, JsonNode node) {
        if (node != null && node.isArray()) {
            node.forEach(item -> text.append("- ").append(item.asText()).append("\n"));
        } else {
            text.append("- （模型未返回该项）\n");
        }
    }

    /**
     * 生成HTML报告
     */
    private String generateHtmlReport(JsonNode reportContent) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>面试评估报告</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .header { text-align: center; margin-bottom: 30px; }
                        .score { font-size: 48px; color: #4CAF50; font-weight: bold; }
                        .section { margin: 20px 0; }
                        .dimension-bar {
                            background: #e0e0e0;
                            height: 20px;
                            border-radius: 10px;
                            margin: 5px 0;
                        }
                        .dimension-fill {
                            background: #4CAF50;
                            height: 100%%;
                            border-radius: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>面试评估报告</h1>
                        <div class="score">%.1f/10</div>
                        <p>%s</p>
                    </div>
                    <div class="section">
                        <h2>总体评价</h2>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """,
                reportContent.path("overall_score").asDouble(0),
                reportContent.path("pass_status").asBoolean(false) ? "✓ 通过" : "✗ 未通过",
                reportContent.path("summary").asText("(无)")
        );
    }

    /**
     * 清理JSON响应
     */
    private String cleanJsonResponse(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        response = response.trim();
        int first = response.indexOf('{');
        int last = response.lastIndexOf('}');
        if (first >= 0 && last > first) {
            response = response.substring(first, last + 1);
        }
        return response.trim();
    }

    /**
     * 估算token数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文约1.5字符/token，英文约4字符/token
        long chineseChars = text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
        long otherChars = text.length() - chineseChars;
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 转换为VO
     */
    private ReportDetailRspVO convertToVO(InterviewReportDO report) {
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

    /**
     * 获取会话
     */
    private InterviewSessionDO getSessionById(UUID sessionId) {
        LambdaQueryWrapper<InterviewSessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSessionDO::getSessionId, sessionId);
        InterviewSessionDO session = sessionMapper.selectOne(wrapper);
        if (session == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_SESSION_NOT_FOUND);
        }
        return session;
    }

    /**
     * 获取模板
     */
    private InterviewTemplateDO getTemplateById(UUID templateId) {
        LambdaQueryWrapper<InterviewTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplateDO::getTemplateId, templateId);
        InterviewTemplateDO template = templateMapper.selectOne(wrapper);
        if (template == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    /**
     * 获取对话消息
     */
    private List<ConversationMessageDO> getMessages(UUID sessionId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getSessionId, sessionId)
                .orderByAsc(ConversationMessageDO::getSequenceNumber);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 获取用户
     */
    private UserDO getUserById(Long userId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getId, userId).isNull(UserDO::getDeletedAt);
        UserDO user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new ApiException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 查询某个会话的最新报告
     */
    private InterviewReportDO findLatestReport(UUID sessionId) {
        LambdaQueryWrapper<InterviewReportDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReportDO::getSessionId, sessionId)
                .orderByDesc(InterviewReportDO::getVersion)
                .last("LIMIT 1");
        return reportMapper.selectOne(wrapper);
    }
}
