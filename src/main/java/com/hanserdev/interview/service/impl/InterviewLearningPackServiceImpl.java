package com.hanserdev.interview.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.hanserdev.interview.constants.AIPromptConstants;
import com.hanserdev.interview.model.vo.interview.ExternalLearningLinkRspVO;
import com.hanserdev.interview.model.vo.interview.InterviewLearningPackRspVO;
import com.hanserdev.interview.model.vo.interview.KnowledgeSnippetRspVO;
import com.hanserdev.interview.model.vo.interview.PracticePlanDayRspVO;
import com.hanserdev.interview.model.vo.interview.PracticePlanGeneratedRspVO;
import com.hanserdev.interview.model.vo.interview.ReportDetailRspVO;
import com.hanserdev.interview.model.vo.question.QuestionRecommendationRspVO;
import com.hanserdev.interview.service.InterviewLearningPackService;
import com.hanserdev.interview.service.InterviewReportService;
import com.hanserdev.interview.service.QuestionRecommendationService;
import com.hanserdev.interview.service.support.LearningCuratedLinks;
import com.hanserdev.interview.service.support.ReportWeakPointExtractor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class InterviewLearningPackServiceImpl implements InterviewLearningPackService {

    private static final int MAX_SNIPPETS = 14;
    private static final int MAX_QUESTION_SNIPPETS = 12;

    @Resource
    private InterviewReportService reportService;

    @Resource
    private QuestionRecommendationService questionRecommendationService;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ChatClient dashScopeChatClient;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${interview.learning-pack.kb-similarity-threshold:0.32}")
    private double kbSimilarityThreshold;

    @Value("${interview.learning-pack.question-similarity-threshold:0.30}")
    private double questionSimilarityThreshold;

    @Override
    @Cacheable(cacheNames = "interviewLearningPacks", key = "#sessionId.toString()")
    public InterviewLearningPackRspVO buildLearningPack(UUID sessionId) {
        ReportDetailRspVO report = reportService.getReportBySessionId(sessionId);
        JsonNode content = report.getReportContent();
        JsonNode contentNode = content != null ? content : MissingNode.getInstance();

        List<String> weakPoints = ReportWeakPointExtractor.extractWeakPoints(contentNode);

        QuestionRecommendationRspVO qrec = questionRecommendationService.generateRecommendQuestions(sessionId);

        JsonNode reportPractice = null;
        if (content != null) {
            JsonNode pp = content.get("practice_plan");
            if (pp != null && !pp.isNull() && pp.isObject()) {
                reportPractice = pp;
            }
        }

        String summary = contentNode.path("summary").asText("");

        List<String> kbQueries = weakPoints.isEmpty() ? List.of("技术面试 基础 巩固") : weakPoints;
        List<KnowledgeSnippetRspVO> snippets = searchKnowledgeSnippets(kbQueries);
        List<KnowledgeSnippetRspVO> bankSnippets = searchQuestionSnippets(kbQueries);
        List<ExternalLearningLinkRspVO> links = LearningCuratedLinks.pickForWeakPoints(
                weakPoints.isEmpty() ? List.of("综合提升") : weakPoints);

        PracticePlanGeneratedRspVO plan = generatePracticePlan(weakPoints, summary);

        return InterviewLearningPackRspVO.builder()
                .sessionId(sessionId)
                .weakPoints(qrec.getWeakPoints())
                .summaryReason(qrec.getReason())
                .reportPracticePlan(reportPractice)
                .generatedPlan(plan)
                .knowledgeSnippets(snippets)
                .questionSnippets(bankSnippets)
                .externalLinks(links)
                .recommendedQuestions(qrec.getRecommendedQuestions())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private List<KnowledgeSnippetRspVO> searchKnowledgeSnippets(List<String> queries) {
        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        var kbOnly = fb.eq("doc_type", "kb").build();

        Map<String, KnowledgeSnippetRspVO> dedupe = new LinkedHashMap<>();
        for (String q : queries) {
            if (dedupe.size() >= MAX_SNIPPETS) {
                break;
            }
            try {
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(q)
                                .topK(4)
                                .similarityThreshold(kbSimilarityThreshold)
                                .filterExpression(kbOnly)
                                .build());
                for (Document doc : docs) {
                    if (dedupe.size() >= MAX_SNIPPETS) {
                        break;
                    }
                    String key = Objects.toString(doc.getMetadata().get("kb_source"), "")
                            + "#"
                            + Objects.toString(doc.getMetadata().get("chunk_index"), "0");
                    if (dedupe.containsKey(key)) {
                        continue;
                    }
                    String text = doc.getText() != null ? doc.getText() : "";
                    String excerpt = text.length() > 360 ? text.substring(0, 360) + "…" : text;
                    dedupe.put(key, KnowledgeSnippetRspVO.builder()
                            .title(Objects.toString(doc.getMetadata().get("kb_title"), "知识库片段"))
                            .kbCategory(Objects.toString(doc.getMetadata().get("kb_category"), ""))
                            .excerpt(excerpt)
                            .sourceHint(Objects.toString(doc.getMetadata().get("kb_source"), ""))
                            .matchedWeakPoint(q)
                            .relevanceScore(scoreFromMetadata(doc))
                            .build());
                }
            } catch (Exception e) {
                log.warn("知识库检索失败 query={}", q, e);
            }
        }
        return new ArrayList<>(dedupe.values());
    }

    /**
     * 按薄弱点查询向量库中的面试题（doc_type=question），与 {@link #searchKnowledgeSnippets} 并行供学习包展示。
     */
    private List<KnowledgeSnippetRspVO> searchQuestionSnippets(List<String> queries) {
        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        Filter.Expression questionOnly = fb.eq("doc_type", "question").build();

        Map<String, KnowledgeSnippetRspVO> dedupe = new LinkedHashMap<>();
        for (String q : queries) {
            if (dedupe.size() >= MAX_QUESTION_SNIPPETS) {
                break;
            }
            try {
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(q)
                                .topK(4)
                                .similarityThreshold(questionSimilarityThreshold)
                                .filterExpression(questionOnly)
                                .build());
                for (Document doc : docs) {
                    if (dedupe.size() >= MAX_QUESTION_SNIPPETS) {
                        break;
                    }
                    String qid = Objects.toString(doc.getMetadata().get("question_id"), "");
                    String key = qid.isBlank() ? Objects.toString(doc.getId(), UUID.randomUUID().toString()) : ("q_" + qid);
                    if (dedupe.containsKey(key)) {
                        continue;
                    }
                    String text = doc.getText() != null ? doc.getText() : "";
                    String excerpt = text.length() > 360 ? text.substring(0, 360) + "…" : text;
                    Object qt = doc.getMetadata().get("question_title");
                    String title = qt != null && !qt.toString().isBlank()
                            ? qt.toString()
                            : ("面试题 #" + (qid.isBlank() ? "?" : qid));
                    dedupe.put(key, KnowledgeSnippetRspVO.builder()
                            .title(title)
                            .kbCategory(Objects.toString(doc.getMetadata().get("kb_category"), "面试题库"))
                            .excerpt(excerpt)
                            .sourceHint(qid.isBlank() ? "面试题库" : ("面试题库 · id=" + qid))
                            .matchedWeakPoint(q)
                            .relevanceScore(scoreFromMetadata(doc))
                            .build());
                }
            } catch (Exception e) {
                log.warn("题库向量检索失败 query={}", q, e);
            }
        }
        return new ArrayList<>(dedupe.values());
    }

    private static Double scoreFromMetadata(Document doc) {
        Object score = doc.getMetadata().get("distance");
        if (score instanceof Number) {
            double distance = ((Number) score).doubleValue();
            return Math.max(0, 1 - distance);
        }
        return null;
    }

    private PracticePlanGeneratedRspVO generatePracticePlan(List<String> weakPoints, String summary) {
        String wpStr = weakPoints.isEmpty()
                ? "综合提升技术表达与常见考点"
                : String.join("；", weakPoints.subList(0, Math.min(weakPoints.size(), 10)));
        String sum = summary == null ? "" : summary;
        if (sum.length() > 1200) {
            sum = sum.substring(0, 1200) + "…";
        }
        String prompt = String.format(AIPromptConstants.PRACTICE_PLAN_GENERATION_PROMPT,
                wpStr,
                sum.isBlank() ? "（无）" : sum);
        try {
            String raw = dashScopeChatClient.prompt(prompt).call().content();
            raw = stripJson(raw);
            JsonNode n = objectMapper.readTree(raw);
            PracticePlanGeneratedRspVO vo = new PracticePlanGeneratedRspVO();
            vo.setGoal(n.path("goal").asText("系统巩固薄弱点并提升面试表现力"));
            List<PracticePlanDayRspVO> days = new ArrayList<>();
            JsonNode arr = n.get("days");
            if (arr != null && arr.isArray()) {
                for (JsonNode dayNode : arr) {
                    PracticePlanDayRspVO d = new PracticePlanDayRspVO();
                    d.setDay(dayNode.path("day").asInt(days.size() + 1));
                    d.setTheme(dayNode.path("theme").asText("练习日"));
                    List<String> tasks = new ArrayList<>();
                    JsonNode tarr = dayNode.get("tasks");
                    if (tarr != null && tarr.isArray()) {
                        tarr.forEach(t -> {
                            String line = t.asText().trim();
                            if (!line.isEmpty()) {
                                tasks.add(line);
                            }
                        });
                    }
                    d.setTasks(tasks);
                    days.add(d);
                }
            }
            vo.setDays(days);
            if (days.size() >= 7) {
                return vo;
            }
        } catch (Exception e) {
            log.warn("个性化练习计划 LLM 解析失败，使用模板: {}", e.toString());
        }
        return fallbackPlan(weakPoints);
    }

    private PracticePlanGeneratedRspVO fallbackPlan(List<String> weakPoints) {
        PracticePlanGeneratedRspVO vo = new PracticePlanGeneratedRspVO();
        String focus = weakPoints.isEmpty() ? "通用能力" : weakPoints.get(0);
        vo.setGoal(String.format(Locale.ROOT, "7 日内针对「%s」等进行结构化巩固（模板计划，可稍后重新生成）", focus));

        String[][] themes = {
                {"基础回顾", "整理薄弱点清单", "复习相关官方文档 45 分钟"},
                {"八股深挖", "写出 5 条自问自答", "对照知识库片段查漏补缺"},
                {"项目表述", "STAR 法则口述一个项目", "录音 6 分钟并回听修改"},
                {"场景题", "完成 1 道场景题白板推演", "写出边界条件与监控点"},
                {"刷题/实践", "完成 2 道相关题库题", "总结易错点"},
                {"模拟面试", "限时回答 3 个追问", "请同学或 AI 反馈逻辑跳跃处"},
                {"复盘总结", "写一页本周收获", "更新简历/作品集表述"}
        };

        List<PracticePlanDayRspVO> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            PracticePlanDayRspVO d = new PracticePlanDayRspVO();
            d.setDay(i + 1);
            d.setTheme(themes[i][0]);
            d.setTasks(List.of(themes[i][1], themes[i][2]));
            days.add(d);
        }
        vo.setDays(days);
        return vo;
    }

    private static String stripJson(String response) {
        String r = response.trim();
        if (r.startsWith("```json")) {
            r = r.substring(7);
        }
        if (r.startsWith("```")) {
            r = r.substring(3);
        }
        if (r.endsWith("```")) {
            r = r.substring(0, r.length() - 3);
        }
        r = r.trim();
        int first = r.indexOf('{');
        int last = r.lastIndexOf('}');
        if (first >= 0 && last > first) {
            r = r.substring(first, last + 1);
        }
        return r.trim();
    }
}
