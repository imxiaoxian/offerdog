package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hanserdev.interview.domain.dataobject.ConversationMessageDO;
import com.hanserdev.interview.domain.dataobject.InterviewPlanTemplateDO;
import com.hanserdev.interview.domain.dataobject.InterviewSessionDO;
import com.hanserdev.interview.domain.dataobject.InterviewTemplateDO;
import com.hanserdev.interview.domain.dataobject.UserDO;
import com.hanserdev.interview.domain.mapper.ConversationMessageMapper;
import com.hanserdev.interview.domain.mapper.InterviewPlanTemplateMapper;
import com.hanserdev.interview.domain.mapper.InterviewSessionMapper;
import com.hanserdev.interview.domain.mapper.InterviewTemplateMapper;
import com.hanserdev.interview.domain.mapper.UserMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.dto.interview.ResumeAnalysisDTO;
import com.hanserdev.interview.model.dto.user.UserProfileDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.ConversationMessageRspVO;
import com.hanserdev.interview.model.vo.interview.SessionDetailRspVO;
import com.hanserdev.interview.service.InterviewSessionService;
import com.hanserdev.interview.service.ResumeAnalysisService;
import com.hanserdev.interview.utils.UserSessionUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 面试会话服务实现
 */
@Slf4j
@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    @Resource
    private InterviewSessionMapper sessionMapper;

    @Resource
    private InterviewTemplateMapper templateMapper;

    @Resource
    private InterviewPlanTemplateMapper planTemplateMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private ResumeAnalysisService resumeAnalysisService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionDetailRspVO createSession(Long userId, UUID templateId) {
        log.info("创建面试会话: userId={}, templateId={}", userId, templateId);

        // 1. 获取用户和简历（未填写简历时用占位信息，仍可创建会话）
        UserDO user = getUserById(userId);
        UserResumeDTO resume = resolveResumeForInterview(user);

        // 2. 分析简历
        ResumeAnalysisDTO analysis = resumeAnalysisService.analyzeResume(resume);

        // 3. 获取面试模板
        InterviewTemplateDO template = getTemplateById(templateId);

        // 4. 根据简历选择合适的计划模板
        InterviewPlanTemplateDO planTemplate = selectPlanTemplate(templateId, analysis.getCurrentLevel());

        // 5. 定制化面试计划
        JsonNode customizedPlan = customizeInterviewPlan(planTemplate.getPlanStructure(), resume, analysis);

        // 6. 创建会话
        InterviewSessionDO session = new InterviewSessionDO();
        session.setSessionId(UUID.randomUUID());
        session.setUserId(userId);
        session.setTemplateId(templateId);
        session.setPlanTemplateId(planTemplate.getPlanTemplateId());
        session.setStatus("in_progress");
        session.setInterviewPlan(customizedPlan);
        session.setCurrentQuestionIndex(0);
        session.setQuestionsCompleted(0);
        session.setTotalMessagesCount(0);
        session.setStartTime(LocalDateTime.now());
        session.setExpectedEndTime(LocalDateTime.now().plusMinutes(template.getEstimatedDurationMinutes()));

        // 7. 存储简历摘要到metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("resume_name", resume.getName());
        metadata.put("resume_level", analysis.getCurrentLevel());
        metadata.put("work_years", analysis.getWorkYears());
        metadata.set("technical_stack", objectMapper.valueToTree(analysis.getTechnicalStack()));
        metadata.set("focus_points", objectMapper.valueToTree(analysis.getInterviewFocusPoints()));
        session.setMetadata(metadata);

        // 8. 保存会话
        if (sessionMapper.insert(session) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        log.info("面试会话创建成功: sessionId={}", session.getSessionId());

        // 9. 转换为VO返回
        return convertToDetailVO(session);
    }

    @Override
    public SessionDetailRspVO getSessionDetail(UUID sessionId) {
        InterviewSessionDO session = getSessionById(sessionId);
        return convertToDetailVO(session);
    }

    @Override
    public List<SessionDetailRspVO> getUserSessions(Long userId) {
        LambdaQueryWrapper<InterviewSessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSessionDO::getUserId, userId)
                .orderByDesc(InterviewSessionDO::getCreatedAt);
        List<InterviewSessionDO> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream()
                .map(this::convertToDetailVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionDetailRspVO> getSessionDetailRspVOS(Long userId, HttpSession httpSession) {
        UserSessionDTO userSession = UserSessionUtils.getRequiredUser(httpSession);
        if (!userSession.getUserId().equals(userId)) {
            throw new ApiException(ResponseCodeEnum.USER_ROLE_INVALID);
        }
        return this.getUserSessions(userId);
    }

    @Override
    public List<ConversationMessageRspVO> getSessionMessages(UUID sessionId) {
        // 确保会话存在
        getSessionById(sessionId);

        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getSessionId, sessionId)
                .orderByAsc(ConversationMessageDO::getSequenceNumber);
        List<ConversationMessageDO> messages = conversationMessageMapper.selectList(wrapper);
        return messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endSession(UUID sessionId) {
        log.info("结束面试会话: sessionId={}", sessionId);

        InterviewSessionDO session = getSessionById(sessionId);
        session.setStatus("completed");
        session.setEndTime(LocalDateTime.now());
        session.setDurationMinutes(null);

        if (sessionMapper.updateById(session) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        log.info("面试会话已结束: sessionId={}", sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInterviewPlan(UUID sessionId, String planJson) {
        try {
            InterviewSessionDO session = getSessionById(sessionId);
            JsonNode planNode = objectMapper.readTree(planJson);
            session.setInterviewPlan(planNode);

            if (sessionMapper.updateById(session) != 1) {
                throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
            }

            log.debug("面试计划已更新: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("更新面试计划失败", e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 根据简历定制化面试计划
     */
    private JsonNode customizeInterviewPlan(JsonNode templatePlan, UserResumeDTO resume, ResumeAnalysisDTO analysis) {
        try {
            // 深拷贝模板计划
            ObjectNode customPlan = templatePlan.deepCopy();
            ArrayNode questions = (ArrayNode) customPlan.get("questions");

            if (questions == null) {
                throw new ApiException(ResponseCodeEnum.PARAM_ERROR);
            }

            // 遍历问题，注入候选人信息
            for (int i = 0; i < questions.size(); i++) {
                ObjectNode question = (ObjectNode) questions.get(i);

                // 如果有项目经历，注入项目信息
                if (resume.getWork() != null && resume.getWork().getProjects() != null
                        && i < resume.getWork().getProjects().size()) {
                    UserResumeDTO.ProjectExperience project = resume.getWork().getProjects().get(i);
                    question.put("candidate_project", project.getName());
                    question.put("candidate_role", project.getRole());

                    // 调整初始问题，结合候选人经验
                    String originalQ = question.get("initial_question").asText();
                    String customizedQ = String.format(
                            "%s (我看到你在%s项目中担任%s,可以结合这个项目来说明)",
                            originalQ,
                            project.getName(),
                            project.getRole()
                    );
                    question.put("initial_question", customizedQ);
                }

                // 根据技能栈调整难度
                if (isSkillMatched(question, analysis.getTechnicalStack())) {
                    question.put("difficulty", "high");
                }

                // 设置问题状态
                if (i == 0) {
                    question.put("status", "current");
                } else {
                    question.put("status", "pending");
                }

                // 初始化追问计数
                question.put("follow_up_count", 0);
                question.put("depth_level", 0);
                if (!question.has("max_depth")) {
                    question.put("max_depth", 3);
                }
            }

            // 添加针对性的附加问题（基于AI分析的重点）
            for (String focusPoint : analysis.getInterviewFocusPoints()) {
                if (questions.size() >= 10) {
                    break; // 限制总问题数
                }

                ObjectNode newQuestion = objectMapper.createObjectNode();
                newQuestion.put("question_id", "custom_" + UUID.randomUUID().toString().substring(0, 8));
                newQuestion.put("topic", focusPoint);
                newQuestion.put("initial_question", "请详细介绍" + focusPoint);
                newQuestion.put("status", "pending");
                newQuestion.put("depth_level", 0);
                newQuestion.put("max_depth", 3);
                newQuestion.put("follow_up_count", 0);
                questions.add(newQuestion);
            }

            customPlan.put("total_questions", questions.size());
            customPlan.put("current_question_index", 0);
            customPlan.put("questions_completed", 0);

            return customPlan;

        } catch (Exception e) {
            log.error("定制化面试计划失败", e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 判断问题是否匹配候选人的技能栈
     */
    private boolean isSkillMatched(ObjectNode question, java.util.List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return false;
        }

        String topic = question.has("topic") ? question.get("topic").asText().toLowerCase() : "";
        String questionText = question.has("initial_question") ?
                question.get("initial_question").asText().toLowerCase() : "";

        for (String skill : skills) {
            String lowerSkill = skill.toLowerCase();
            if (topic.contains(lowerSkill) || questionText.contains(lowerSkill)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 解析用于创建面试的简历：库中无简历时构造最小占位，避免直接拒绝创建。
     */
    private UserResumeDTO resolveResumeForInterview(UserDO user) {
        UserResumeDTO existing = user.getResume();
        if (existing != null) {
            return existing;
        }
        UserResumeDTO placeholder = new UserResumeDTO();
        String name = "求职者";
        UserProfileDTO profile = user.getProfile();
        if (profile != null && profile.getRealName() != null && !profile.getRealName().isBlank()) {
            name = profile.getRealName().trim();
        } else if (user.getUsername() != null && !user.getUsername().isBlank()) {
            name = user.getUsername().trim();
        }
        placeholder.setName(name);
        placeholder.setJobIntention("待补充");
        return placeholder;
    }

    /**
     * 选择合适的计划模板
     */
    private InterviewPlanTemplateDO selectPlanTemplate(UUID templateId, String level) {
        LambdaQueryWrapper<InterviewPlanTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewPlanTemplateDO::getTemplateId, templateId)
                .eq(InterviewPlanTemplateDO::getIsActive, true)
                .orderByDesc(InterviewPlanTemplateDO::getUsageCount)
                .last("LIMIT 1");

        InterviewPlanTemplateDO planTemplate = planTemplateMapper.selectOne(wrapper);
        if (planTemplate == null) {
            throw new ApiException(ResponseCodeEnum.PARAM_ERROR);
        }

        return planTemplate;
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
     * 获取模板
     */
    private InterviewTemplateDO getTemplateById(UUID templateId) {
        LambdaQueryWrapper<InterviewTemplateDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplateDO::getTemplateId, templateId)
                .eq(InterviewTemplateDO::getIsActive, true);
        InterviewTemplateDO template = templateMapper.selectOne(wrapper);
        if (template == null) {
            throw new ApiException(ResponseCodeEnum.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        return template;
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

    private ConversationMessageRspVO convertToMessageVO(ConversationMessageDO message) {
        ConversationMessageRspVO vo = new ConversationMessageRspVO();
        vo.setId(message.getMessageId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole() != null ? message.getRole().getDbValue() : null);
        vo.setContent(message.getContent());
        vo.setRelatedQuestionId(message.getRelatedQuestionId());
        vo.setSequenceNumber(message.getSequenceNumber());
        vo.setTokenCount(message.getTokenCount() != null
                ? message.getTokenCount()
                : message.getEstimatedTokens());
        vo.setTimestamp(message.getTimestamp());
        return vo;
    }

    /**
     * 转换为VO
     */
    private SessionDetailRspVO convertToDetailVO(InterviewSessionDO session) {
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
}
