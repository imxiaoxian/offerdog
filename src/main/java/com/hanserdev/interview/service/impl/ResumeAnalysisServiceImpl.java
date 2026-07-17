package com.hanserdev.interview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanserdev.interview.constants.AIPromptConstants;
import com.hanserdev.interview.model.dto.interview.ResumeAnalysisDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.service.ResumeAnalysisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.stream.Collectors;

/**
 * 简历分析服务实现
 */
@Slf4j
@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    @Resource
    private ChatClient dashScopeChatClient;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public ResumeAnalysisDTO analyzeResume(UserResumeDTO resume) {
        if (resume.getName() == null || resume.getName().isBlank()) {
            resume.setName("求职者");
        }
        log.info("开始分析简历: {}", resume.getName());

        // 构建提示词
        String prompt = buildResumeAnalysisPrompt(resume);

        try {
            // 调用AI分析
            String aiResponse = dashScopeChatClient.prompt(prompt).call().content();

            // 清理可能的markdown标记
            aiResponse = cleanJsonResponse(aiResponse);

            // 解析JSON响应
            ResumeAnalysisDTO analysis = objectMapper.readValue(aiResponse, ResumeAnalysisDTO.class);

            log.info("简历分析完成: {} - 级别: {}, 工作年限: {}年",
                    resume.getName(), analysis.getCurrentLevel(), analysis.getWorkYears());

            return analysis;

        } catch (Exception e) {
            log.error("简历分析失败", e);
            // 返回默认分析结果
            return buildDefaultAnalysis(resume);
        }
    }

    /**
     * 构建简历分析提示词
     */
    private String buildResumeAnalysisPrompt(UserResumeDTO resume) {
        // 计算工作年限
        int workYears = calculateWorkYears(resume.getWork() != null ?
                resume.getWork().getFirstEmployment() : null);

        // 获取最近的工作经历
        String currentTitle = "";
        String currentCompany = "";
        if (resume.getWork() != null && resume.getWork().getExperience() != null
                && !resume.getWork().getExperience().isEmpty()) {
            UserResumeDTO.WorkExperience latestWork = resume.getWork().getExperience().get(0);
            currentTitle = latestWork.getTitle();
            currentCompany = latestWork.getCompany();
        }

        // 格式化工作经历
        String workExperience = formatWorkExperience(resume.getWork());

        // 格式化项目经历
        String projectExperience = formatProjectExperience(resume.getWork());

        return String.format(
                AIPromptConstants.RESUME_ANALYSIS_PROMPT,
                resume.getName(),
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getLevel() : "本科",
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getMajor() : "计算机相关",
                resume.getEducation() != null && !resume.getEducation().isEmpty() ? resume.getEducation().get(0).getSchool() : "未知",
                workYears,
                currentTitle,
                currentCompany,
                resume.getSkills() != null ? String.join(", ", resume.getSkills()) : "无",
                resume.getJobIntention() != null ? resume.getJobIntention() : "无",
                workExperience,
                projectExperience
        );
    }

    /**
     * 计算工作年限
     */
    private int calculateWorkYears(YearMonth firstEmployment) {
        if (firstEmployment == null) {
            return 0;
        }
        LocalDate first = firstEmployment.atDay(1);
        LocalDate now = LocalDate.now();
        return Period.between(first, now).getYears();
    }

    /**
     * 格式化工作经历
     */
    private String formatWorkExperience(UserResumeDTO.Work work) {
        if (work == null || work.getExperience() == null || work.getExperience().isEmpty()) {
            return "无工作经历";
        }

        return work.getExperience().stream()
                .map(exp -> String.format("- %s @ %s (%s - %s)\n  %s",
                        exp.getTitle(),
                        exp.getCompany(),
                        exp.getStartDate(),
                        exp.getEndDate() != null ? exp.getEndDate() : "至今",
                        exp.getDescription()))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 格式化项目经历
     */
    private String formatProjectExperience(UserResumeDTO.Work work) {
        if (work == null || work.getProjects() == null || work.getProjects().isEmpty()) {
            return "无项目经历";
        }

        return work.getProjects().stream()
                .map(project -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("- %s (角色: %s)\n  %s",
                            project.getName(),
                            project.getRole(),
                            project.getDescription()));

                    if (project.getHighlights() != null && !project.getHighlights().isEmpty()) {
                        sb.append("\n  亮点:\n");
                        project.getHighlights().forEach(h -> sb.append("  * ").append(h).append("\n"));
                    }

                    return sb.toString();
                })
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 清理JSON响应（去除markdown标记）
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
        return response.trim();
    }

    /**
     * 构建默认分析结果（当AI分析失败时）
     */
    private ResumeAnalysisDTO buildDefaultAnalysis(UserResumeDTO resume) {
        ResumeAnalysisDTO analysis = new ResumeAnalysisDTO();

        // 基于工作年限判断级别
        int workYears = calculateWorkYears(resume.getWork() != null ?
                resume.getWork().getFirstEmployment() : null);
        analysis.setWorkYears(workYears);

        if (workYears < 2) {
            analysis.setCurrentLevel("junior");
        } else if (workYears < 5) {
            analysis.setCurrentLevel("mid");
        } else if (workYears < 8) {
            analysis.setCurrentLevel("senior");
        } else {
            analysis.setCurrentLevel("high_senior");
        }

        // 技术栈
        analysis.setTechnicalStack(resume.getSkills() != null ? resume.getSkills() : java.util.Collections.emptyList());

        // 默认重点和建议
        analysis.setStrongAreas(java.util.Collections.singletonList("综合技术能力"));
        analysis.setWeakAreas(java.util.Collections.emptyList());
        analysis.setInterviewFocusPoints(java.util.Collections.singletonList("项目经验和技术深度"));
        analysis.setSuggestedQuestions(java.util.Collections.singletonList("请介绍一下您最有成就感的项目"));

        return analysis;
    }
}

