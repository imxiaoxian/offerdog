package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hanserdev.interview.constants.AIPromptConstants;
import com.hanserdev.interview.domain.dataobject.ConversationMessageDO;
import com.hanserdev.interview.domain.dataobject.InterviewSessionDO;
import com.hanserdev.interview.domain.dataobject.InterviewTemplateDO;
import com.hanserdev.interview.domain.mapper.ConversationMessageMapper;
import com.hanserdev.interview.domain.mapper.InterviewSessionMapper;
import com.hanserdev.interview.domain.mapper.InterviewTemplateMapper;
import com.hanserdev.interview.enums.ConversationRoleEnum;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.dto.interview.InterviewActionDTO;
import com.hanserdev.interview.model.dto.interview.SessionContextDTO;
import com.hanserdev.interview.model.vo.interview.AIResponseRspVO;
import com.hanserdev.interview.model.vo.interview.InterviewTemplateRspVO;
import com.hanserdev.interview.service.InterviewAIService;
import com.hanserdev.interview.service.InterviewAudioService;
import com.hanserdev.interview.service.InterviewRagService;
import com.hanserdev.interview.utils.MarkdownUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI面试服务实现
 */
@Slf4j
@Service
public class InterviewAIServiceImpl implements InterviewAIService {

    @Resource
    private InterviewSessionMapper sessionMapper;

    @Resource
    private InterviewTemplateMapper templateMapper;

    @Resource
    private ConversationMessageMapper messageMapper;

    @Resource
    private ChatClient dashScopeChatClient;


    @Resource
    private InterviewAudioService interviewAudioService;

    @Resource
    private InterviewRagService interviewRagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateOpening(UUID sessionId) {
        log.info("生成开场白: sessionId={}", sessionId);

        // 1. 获取会话和模板信息
        InterviewSessionDO session = getSessionById(sessionId);
        InterviewTemplateDO template = getTemplateById(session.getTemplateId());

        String opening;
        try {
            String prompt = buildOpeningPrompt(session, template);
            opening = dashScopeChatClient.prompt(prompt).call().content();
            if (opening == null || opening.isBlank()) {
                opening = buildFallbackOpening(session, template);
            }
        } catch (Exception e) {
            log.warn("大模型生成开场白失败，使用本地模板: sessionId={}", sessionId, e);
            opening = buildFallbackOpening(session, template);
        }

        int sequenceNumber = getNextSequenceNumber(sessionId);
        saveMessage(sessionId, ConversationRoleEnum.INTERVIEWER, opening, getFirstQuestionId(session), sequenceNumber);

        String plain = opening.replace("#", "")
                .replace("*", "")
                .replace("-", "")
                .replaceAll("(?m)^[ \t]*\r?\n", "");
        try {
            interviewAudioService.streamInterviewerSpeech(sessionId, sequenceNumber, MarkdownUtils.toPlainText(plain));
        } catch (Exception e) {
            log.debug("开场白语音推送跳过: sessionId={}", sessionId, e);
        }

        log.info("开场白已就绪: sessionId={}", sessionId);
        return opening;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AIResponseRspVO generateResponse(UUID sessionId, String questionId, String answer) {
        log.info("生成AI响应: sessionId={}, questionId={}", sessionId, questionId);

        // 1. 保存用户回答
        int nextSeq = getNextSequenceNumber(sessionId);
        saveMessage(sessionId, ConversationRoleEnum.CANDIDATE, answer, questionId, nextSeq);

        try {
            // 2. 构建会话上下文与提示词（与 LLM 调用同一 try，便于统一记录根因）
            SessionContextDTO context = buildSessionContext(sessionId);
            String prompt = buildConversationPrompt(context);

            // 3. 调用AI生成响应
            String aiResponse = dashScopeChatClient.prompt(prompt).call().content();
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("大模型返回空内容: sessionId={}", sessionId);
                aiResponse = "感谢你的回答，请再具体说明一下你在该场景下的做法与思考。";
            }

            // 4. 解析动作标记
            InterviewActionDTO action = parseAction(aiResponse);
            String cleanResponse = removeActionTag(aiResponse);

            // 5. 保存AI消息
            int aiSeq = nextSeq + 1;
            saveMessage(sessionId, ConversationRoleEnum.INTERVIEWER, cleanResponse, questionId, aiSeq);

            String s = cleanResponse.replace("#", "")
                    .replace("*", "")
                    .replace("-", "")
                    .replaceAll("(?m)^[ \t]*\r?\n", "");// 移除纯空白行

            // 6. 语音播报失败不影响答题主流程（未配置豆包 TTS 时常在此跳过）
            try {
                interviewAudioService.streamInterviewerSpeech(sessionId, aiSeq, s);
            } catch (Exception audioEx) {
                log.debug("面试官语音推送跳过: sessionId={}, seq={}", sessionId, aiSeq, audioEx);
            }

            // 7. 更新面试计划状态
            updateInterviewPlan(sessionId, action);

            // 8. 构建响应VO
            AIResponseRspVO vo = AIResponseRspVO.builder()
                    .content(cleanResponse)
                    .action(action.getType())
                    .currentQuestionIndex(context.getSession().getCurrentQuestionIndex())
                    .completed(action.isInterviewCompleted())
                    .sequenceNumber(aiSeq)
                    .build();

            log.info("AI响应生成成功: sessionId={}, action={}", sessionId, action.getType());
            return vo;

        } catch (ApiException e) {
            throw e;
        } catch (IllegalStateException e) {
            log.warn("会话状态与面试计划不一致: sessionId={}, questionId={}, msg={}",
                    sessionId, questionId, e.getMessage());
            throw new ApiException(ResponseCodeEnum.PARAM_ERROR, e.getMessage());
        } catch (Exception e) {
            ApiException llm = mapLlmFailure(e);
            if (llm != null) {
                throw llm;
            }
            log.error("生成AI响应失败: sessionId={}, questionId={}, cause={}",
                    sessionId, questionId, e.toString(), e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 将 DeepSeek / Spring AI 调用失败映射为可诊断的业务错误；无法识别时返回 null 由上层记日志并返回 10000。
     */
    private ApiException mapLlmFailure(Throwable e) {
        String msg = deepMessage(e);
        if (msg == null) {
            return null;
        }
        String lower = msg.toLowerCase();
        if (msg.contains("401")
                || lower.contains("unauthorized")
                || lower.contains("invalid_api_key")
                || lower.contains("incorrect api key")
                || msg.contains("无效的令牌")
                || msg.contains("无权限")) {
            return new ApiException(ResponseCodeEnum.LLM_CALL_FAILED,
                    "AI 接口鉴权失败，请检查 DEEPSEEK_API_KEY 是否有效并已注入后端");
        }
        if (msg.contains("429") || lower.contains("rate limit") || lower.contains("too many requests")) {
            return new ApiException(ResponseCodeEnum.LLM_CALL_FAILED, "AI 接口请求过于频繁，请稍后重试");
        }
        if (lower.contains("connection refused")
                || lower.contains("timed out")
                || lower.contains("timeout")
                || lower.contains("failed to respond")) {
            return new ApiException(ResponseCodeEnum.LLM_CALL_FAILED,
                    "无法连接 AI 服务，请检查网络或 DEEPSEEK_BASE_URL");
        }
        return null;
    }

    private static String deepMessage(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable t = e;
        String last = t.getMessage();
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
            if (t.getMessage() != null) {
                last = t.getMessage();
            }
        }
        return last;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInterviewPlan(UUID sessionId, InterviewActionDTO action) {
        try {
            InterviewSessionDO session = getSessionById(sessionId);
            JsonNode planNode = session.getInterviewPlan();
            if (planNode == null || !planNode.isObject()) {
                log.warn("updateInterviewPlan: 无 interviewPlan，跳过: sessionId={}", sessionId);
                return;
            }
            ObjectNode plan = (ObjectNode) planNode;
            if (!plan.has("questions") || !plan.get("questions").isArray()) {
                log.warn("updateInterviewPlan: plan 无 questions 数组，跳过: sessionId={}", sessionId);
                return;
            }
            int currentIdx = session.getCurrentQuestionIndex();
            if (currentIdx < 0 || currentIdx >= plan.get("questions").size()) {
                log.warn("updateInterviewPlan: 题目索引无效 idx={}，跳过: sessionId={}", currentIdx, sessionId);
                return;
            }

            int totalQ = plan.path("total_questions").asInt(plan.get("questions").size());

            switch (action.getType()) {
                case FOLLOW_UP:
                    // 增加深度和追问次数
                    ObjectNode currentQ = (ObjectNode) plan.get("questions").get(currentIdx);
                    currentQ.put("depth_level", currentQ.path("depth_level").asInt(0) + 1);
                    currentQ.put("follow_up_count", currentQ.path("follow_up_count").asInt(0) + 1);
                    break;

                case NEXT_QUESTION:
                    // 标记当前问题完成
                    ObjectNode completedQ = (ObjectNode) plan.get("questions").get(currentIdx);
                    completedQ.put("status", "completed");
                    completedQ.put("end_time", LocalDateTime.now().toString());

                    // 标记下一个问题为当前
                    if (currentIdx + 1 < totalQ && currentIdx + 1 < plan.get("questions").size()) {
                        ObjectNode nextQ = (ObjectNode) plan.get("questions").get(currentIdx + 1);
                        nextQ.put("status", "current");
                        nextQ.put("start_time", LocalDateTime.now().toString());

                        session.setCurrentQuestionIndex(currentIdx + 1);
                    }
                    int done = session.getQuestionsCompleted() != null ? session.getQuestionsCompleted() : 0;
                    session.setQuestionsCompleted(done + 1);
                    break;

                case END_INTERVIEW:
                    // 标记会话完成
                    session.setStatus("completed");
                    session.setEndTime(LocalDateTime.now());
                    break;

                default:
                    // CONTINUE - 不做任何改动
                    break;
            }

            session.setInterviewPlan(plan);
            sessionMapper.updateById(session);

            log.debug("面试计划已更新: sessionId={}, action={}", sessionId, action.getType());

        } catch (Exception e) {
            log.error("更新面试计划失败", e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public List<InterviewTemplateRspVO> getInterviewTemplateRspVOS() {
        LambdaQueryWrapper<InterviewTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplateDO::getIsActive, true)
                .orderByAsc(InterviewTemplateDO::getCategory)
                .orderByAsc(InterviewTemplateDO::getDifficultyLevel);

        List<InterviewTemplateDO> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将DO转换为VO
     */
    private InterviewTemplateRspVO convertToVO(InterviewTemplateDO template) {
        InterviewTemplateRspVO vo = new InterviewTemplateRspVO();
        vo.setId(template.getTemplateId());
        vo.setTemplateName(template.getTemplateName());
        vo.setDescription(template.getDescription());
        vo.setDifficultyLevel(template.getDifficultyLevel());
        vo.setEstimatedDurationMinutes(template.getEstimatedDurationMinutes());
        vo.setCategory(template.getCategory());
        vo.setSystemPrompt(template.getSystemPrompt());
        vo.setIsActive(template.getIsActive());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }

    /**
     * 大模型不可用时使用的简短开场白（仍包含第一题，保证流程可继续）
     */
    private String buildFallbackOpening(InterviewSessionDO session, InterviewTemplateDO template) {
        String name = "候选人";
        JsonNode meta = session.getMetadata();
        if (meta != null && meta.has("resume_name") && !meta.get("resume_name").isNull()) {
            String n = meta.get("resume_name").asText();
            if (n != null && !n.isBlank()) {
                name = n.trim();
            }
        }
        int minutes = template.getEstimatedDurationMinutes() != null ? template.getEstimatedDurationMinutes() : 30;
        String firstQuestion = "请先做一个简短的自我介绍，并说明您应聘本岗位的优势。";
        JsonNode plan = session.getInterviewPlan();
        if (plan != null && plan.has("questions") && plan.get("questions").isArray() && plan.get("questions").size() > 0) {
            JsonNode q0 = plan.get("questions").get(0);
            if (q0 != null && q0.has("initial_question")) {
                firstQuestion = q0.get("initial_question").asText(firstQuestion);
            }
        }
        return String.format(
                "%s您好，欢迎参加「%s」模拟面试，预计时长约 %d 分钟。下面进入第一题：%s",
                name,
                template.getTemplateName(),
                minutes,
                firstQuestion
        );
    }

    /**
     * 构建开场白提示词
     */
    private String buildOpeningPrompt(InterviewSessionDO session, InterviewTemplateDO template) {
        JsonNode metadata = session.getMetadata();
        JsonNode interviewPlan = session.getInterviewPlan();
        if (metadata == null || interviewPlan == null || !interviewPlan.has("questions")
                || !interviewPlan.get("questions").isArray() || interviewPlan.get("questions").size() == 0) {
            throw new IllegalStateException("会话缺少面试计划或题目列表");
        }
        JsonNode firstQuestion = interviewPlan.get("questions").get(0);

        String resumeName = metadata.has("resume_name") && !metadata.get("resume_name").isNull()
                ? metadata.get("resume_name").asText("候选人") : "候选人";
        int workYears = metadata.has("work_years") && metadata.get("work_years").isNumber()
                ? metadata.get("work_years").asInt() : 0;
        String resumeLevel = metadata.has("resume_level") && !metadata.get("resume_level").isNull()
                ? metadata.get("resume_level").asText("mid") : "mid";
        String technicalStack = metadata.has("technical_stack") ? metadata.get("technical_stack").toString() : "[]";
        String focusPoints = metadata.has("focus_points") ? metadata.get("focus_points").toString() : "[]";
        int totalQuestions = interviewPlan.has("total_questions")
                ? interviewPlan.get("total_questions").asInt(1) : 1;
        int estMinutes = template.getEstimatedDurationMinutes() != null ? template.getEstimatedDurationMinutes() : 30;
        String initialQ = firstQuestion.has("initial_question")
                ? firstQuestion.get("initial_question").asText("请自我介绍") : "请自我介绍";

        return String.format(
                AIPromptConstants.OPENING_PROMPT_TEMPLATE,
                template.getSystemPrompt(),
                resumeName,
                workYears,
                resumeLevel,
                technicalStack,
                focusPoints,
                totalQuestions,
                estMinutes,
                initialQ
        );
    }

    /**
     * 构建对话提示词
     */
    private String buildConversationPrompt(SessionContextDTO context) {
        InterviewSessionDO session = context.getSession();
        InterviewTemplateDO template = getTemplateById(session.getTemplateId());

        JsonNode metadata = session.getMetadata();
        JsonNode plan = session.getInterviewPlan();
        if (plan == null || !plan.has("questions") || !plan.get("questions").isArray()
                || plan.get("questions").size() == 0) {
            throw new IllegalStateException("会话缺少面试计划或题目列表");
        }
        int currentIdx = session.getCurrentQuestionIndex();
        if (currentIdx < 0 || currentIdx >= plan.get("questions").size()) {
            throw new IllegalStateException("当前题目索引越界: index=" + currentIdx);
        }
        JsonNode currentQ = plan.get("questions").get(currentIdx);

        String resumeName = metadata != null ? metadata.path("resume_name").asText("候选人") : "候选人";
        int workYears = metadata != null ? metadata.path("work_years").asInt(0) : 0;
        String resumeLevel = metadata != null ? metadata.path("resume_level").asText("mid") : "mid";
        String techJoined = String.join(", ", toStringList(metadata != null ? metadata.get("technical_stack") : null));
        String techJoinedCn = String.join("、", toStringList(metadata != null ? metadata.get("technical_stack") : null));

        // 构建候选人项目背景
        String projectContext = "";
        if (currentQ.has("candidate_project") && !currentQ.get("candidate_project").isNull()) {
            projectContext = String.format(
                    AIPromptConstants.PROJECT_CONTEXT_TEMPLATE,
                    currentQ.path("candidate_project").asText(""),
                    currentQ.path("candidate_role").asText("")
            );
        }

        // 获取下一题预览
        String nextQuestionPreview = getNextQuestionPreview(plan, currentIdx);

        // 格式化对话历史
        String conversationHistory = formatConversationHistory(context.getMessages());

        String ragContext = interviewRagService.buildRagContextBlock(
                currentQ.path("topic").asText(""),
                currentQ.path("initial_question").asText(""),
                getLastCandidateAnswer(context));

        // 计算已用时长
        long elapsedMinutes = 0L;
        if (session.getStartTime() != null) {
            elapsedMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), LocalDateTime.now());
        }

        return String.format(
                AIPromptConstants.CONVERSATION_PROMPT_TEMPLATE,
                template.getSystemPrompt(),
                resumeName,
                workYears,
                resumeLevel,
                techJoined,
                projectContext,
                ragContext,
                currentIdx + 1,
                plan.path("total_questions").asInt(1),
                session.getQuestionsCompleted() != null ? session.getQuestionsCompleted() : 0,
                elapsedMinutes,
                currentQ.path("question_id").asText(""),
                currentQ.path("topic").asText(""),
                currentQ.path("initial_question").asText(""),
                currentQ.path("depth_level").asInt(0),
                currentQ.path("max_depth").asInt(3),
                currentQ.path("follow_up_count").asInt(0),
                resumeLevel,
                currentQ.path("max_depth").asInt(3),
                nextQuestionPreview,
                resumeName,
                techJoinedCn,
                conversationHistory
        );
    }

    private static String getLastCandidateAnswer(SessionContextDTO context) {
        List<ConversationMessageDO> messages = context.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessageDO m = messages.get(i);
            if (m.getRole() == ConversationRoleEnum.CANDIDATE) {
                return m.getContent() != null ? m.getContent() : "";
            }
        }
        return "";
    }

    /**
     * 构建会话上下文
     */
    private SessionContextDTO buildSessionContext(UUID sessionId) {
        InterviewSessionDO session = getSessionById(sessionId);

        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getSessionId, sessionId)
                .orderByDesc(ConversationMessageDO::getSequenceNumber)
                .last("LIMIT 20"); // 只保留最近20条消息

        List<ConversationMessageDO> messages = messageMapper.selectList(wrapper);
        // 反转顺序，使其按时间正序
        java.util.Collections.reverse(messages);

        // 获取当前问题ID
        String currentQuestionId = "";
        JsonNode plan = session.getInterviewPlan();
        if (plan != null && plan.has("questions") && plan.get("questions").isArray()) {
            int idx = session.getCurrentQuestionIndex();
            if (idx >= 0 && idx < plan.get("questions").size()) {
                currentQuestionId = plan.get("questions").get(idx).path("question_id").asText("");
            }
        }

        return SessionContextDTO.builder()
                .session(session)
                .messages(messages)
                .currentQuestionId(currentQuestionId)
                .build();
    }

    /**
     * 格式化对话历史
     */
    private String formatConversationHistory(List<ConversationMessageDO> messages) {
        if (messages == null || messages.isEmpty()) {
            return "暂无对话历史";
        }

        return messages.stream()
                .map(msg -> String.format("[%s]: %s",
                        msg.getRole() == ConversationRoleEnum.INTERVIEWER ? "面试官" : "候选人",
                        msg.getContent()))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 获取下一题预览
     */
    private String getNextQuestionPreview(JsonNode plan, int currentIdx) {
        JsonNode questions = plan.get("questions");
        if (questions == null || !questions.isArray() || questions.size() == 0) {
            return "已是最后一个问题";
        }
        int total = plan.path("total_questions").asInt(questions.size());
        int nextIdx = currentIdx + 1;
        // total_questions 可能与 questions 数组长度不一致，避免 NPE
        if (nextIdx >= questions.size() || nextIdx >= total) {
            return "已是最后一个问题";
        }
        JsonNode nextQ = questions.get(nextIdx);
        if (nextQ == null || nextQ.isNull()) {
            return "已是最后一个问题";
        }
        return String.format("主题: %s, 问题: %s",
                nextQ.path("topic").asText(""),
                nextQ.path("initial_question").asText(""));
    }

    /**
     * 解析AI返回的动作标记
     */
    private InterviewActionDTO parseAction(String aiResponse) {
        if (aiResponse == null) {
            return InterviewActionDTO.builder()
                    .type(InterviewActionDTO.ActionType.CONTINUE)
                    .build();
        }
        String s = aiResponse.stripLeading();
        if (s.startsWith("[ACTION:FOLLOW_UP]")) {
            return InterviewActionDTO.builder()
                    .type(InterviewActionDTO.ActionType.FOLLOW_UP)
                    .build();
        } else if (s.startsWith("[ACTION:NEXT_QUESTION]")) {
            return InterviewActionDTO.builder()
                    .type(InterviewActionDTO.ActionType.NEXT_QUESTION)
                    .build();
        } else if (s.startsWith("[ACTION:END_INTERVIEW]")) {
            return InterviewActionDTO.builder()
                    .type(InterviewActionDTO.ActionType.END_INTERVIEW)
                    .interviewCompleted(true)
                    .build();
        }

        // 默认继续当前问题
        return InterviewActionDTO.builder()
                .type(InterviewActionDTO.ActionType.CONTINUE)
                .build();
    }

    /**
     * 移除动作标记
     */
    private String removeActionTag(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return "";
        }
        String t = aiResponse.stripLeading();
        if (t.startsWith("[ACTION:")) {
            int endIdx = t.indexOf("]");
            if (endIdx > 0) {
                return t.substring(endIdx + 1).trim();
            }
        }
        return aiResponse.trim();
    }

    /**
     * 保存消息
     */
    private void saveMessage(UUID sessionId, ConversationRoleEnum role, String content,
                             String questionId, int sequence) {
        ConversationMessageDO message = new ConversationMessageDO();
        message.setMessageId(UUID.randomUUID());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setRelatedQuestionId(questionId);
        message.setSequenceNumber(sequence);
        message.setTokenCount(estimateTokens(content));
        message.setEstimatedTokens(estimateTokens(content));
        message.setTimestamp(LocalDateTime.now());

        messageMapper.insert(message);
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
     * 获取下一个序列号
     */
    private int getNextSequenceNumber(UUID sessionId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getSessionId, sessionId)
                .orderByDesc(ConversationMessageDO::getSequenceNumber)
                .last("LIMIT 1");

        ConversationMessageDO lastMessage = messageMapper.selectOne(wrapper);
        return lastMessage != null ? lastMessage.getSequenceNumber() + 1 : 1;
    }

    /**
     * 获取第一个问题ID
     */
    private String getFirstQuestionId(InterviewSessionDO session) {
        JsonNode plan = session.getInterviewPlan();
        if (plan != null && plan.has("questions")) {
            JsonNode firstQ = plan.get("questions").get(0);
            return firstQ.get("question_id").asText();
        }
        return "q1";
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
     * JsonNode转字符串列表
     */
    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return java.util.Collections.emptyList();
        }

        java.util.List<String> list = new java.util.ArrayList<>();
        node.forEach(n -> list.add(n.asText()));
        return list;
    }
}
