package com.hanserdev.interview.model.vo.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.hanserdev.interview.model.vo.question.RecommendedQuestionVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 智能学习包：知识点片段、外链、个性化练习计划与推荐面试题。
 */
@Data
@Builder
public class InterviewLearningPackRspVO {

    private UUID sessionId;
    private List<String> weakPoints;
    private String summaryReason;

    /** 报告中若已有 practice_plan，原样带回供对照 */
    private JsonNode reportPracticePlan;

    /** 大模型生成的 7 日计划 */
    private PracticePlanGeneratedRspVO generatedPlan;

    private List<KnowledgeSnippetRspVO> knowledgeSnippets;
    /** 面试题库向量检索片段（doc_type=question），结构与知识库片段一致便于前端复用 */
    private List<KnowledgeSnippetRspVO> questionSnippets;
    private List<ExternalLearningLinkRspVO> externalLinks;
    private List<RecommendedQuestionVO> recommendedQuestions;

    private LocalDateTime generatedAt;
}
