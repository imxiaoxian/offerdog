package com.hanserdev.interview.service.impl;

import com.hanserdev.interview.service.InterviewRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 面试 RAG：同时检索知识库（{@code doc_type=kb}）与题库题目（{@code doc_type=question}）。
 * 知识库可按主题推断大类缩小 {@code kb_category}；题库仅按语义检索，避免题目索引大类与模板不一致导致无命中。
 */
@Slf4j
@Service
public class InterviewRagServiceImpl implements InterviewRagService {

    @Resource
    private VectorStore vectorStore;

    @Value("${interview.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${interview.rag.top-k:6}")
    private int topK;

    @Value("${interview.rag.similarity-threshold:0.35}")
    private double similarityThreshold;

    @Override
    public String buildRagContextBlock(String topic, String initialQuestion, String lastCandidateAnswer) {
        if (!ragEnabled) {
            return "（知识库检索已关闭。）";
        }
        String t = topic != null ? topic : "";
        String q = initialQuestion != null ? initialQuestion : "";
        String a = lastCandidateAnswer != null ? lastCandidateAnswer : "";
        String query = (t + "\n" + q + "\n" + a).trim();
        if (query.isBlank()) {
            query = "技术面试";
        }

        String category = inferCategory(t + " " + q);
        int kbK = Math.max(2, (topK + 1) / 2);
        int bankK = Math.max(2, topK - kbK);

        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        Filter.Expression kbFilter = buildKbCategoryFilter(fb, category);
        // 题库向量上的 kb_category 由题干推断且常有默认值，与面试模板大类不一致会导致检索为空；仅用语义检索 + doc_type 即可
        Filter.Expression bankFilter = fb.eq("doc_type", "question").build();

        try {
            List<Document> kbDocs = similaritySearchSafe(query, kbFilter, kbK);
            List<Document> bankDocs = similaritySearchSafe(query, bankFilter, bankK);
            if (kbDocs.isEmpty() && bankDocs.isEmpty()) {
                return "（暂无匹配知识库与题库片段，可依据通用经验追问与点评。）";
            }
            StringBuilder sb = new StringBuilder();
            if (!kbDocs.isEmpty()) {
                sb.append("#### 知识库片段\n\n");
                appendKbDocs(sb, kbDocs);
            }
            if (!bankDocs.isEmpty()) {
                sb.append("#### 题库参考（含参考答案要点）\n\n");
                appendBankDocs(sb, bankDocs);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("RAG 检索失败（忽略并继续面试）: {}", e.toString());
            return "（知识库/题库检索暂时不可用。）";
        }
    }

    private List<Document> similaritySearchSafe(String query, Filter.Expression filter, int k) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(k)
                            .similarityThreshold(similarityThreshold)
                            .filterExpression(filter)
                            .build());
        } catch (Exception e) {
            log.warn("向量检索子查询失败: {}", e.toString());
            return new ArrayList<>();
        }
    }

    private static Filter.Expression buildKbCategoryFilter(FilterExpressionBuilder fb, String category) {
        if (category != null && !category.isBlank()) {
            return fb.and(fb.eq("doc_type", "kb"), fb.eq("kb_category", category)).build();
        }
        return fb.eq("doc_type", "kb").build();
    }

    private static void appendKbDocs(StringBuilder sb, List<Document> docs) {
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            Object titleObj = d.getMetadata().get("kb_title");
            String title = titleObj != null ? titleObj.toString() : "片段" + (i + 1);
            sb.append("- **").append(title).append("**\n").append(d.getText()).append("\n\n");
        }
    }

    private static void appendBankDocs(StringBuilder sb, List<Document> docs) {
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            Object qt = d.getMetadata().get("question_title");
            Object qid = d.getMetadata().get("question_id");
            String title = qt != null && !qt.toString().isBlank()
                    ? qt.toString()
                    : ("题库题目 #" + (qid != null ? qid : (i + 1)));
            sb.append("- **").append(title).append("**\n").append(d.getText()).append("\n\n");
        }
    }

    /**
     * 根据题干主题推断知识库大类；无法推断时不限定类别（仍只限 doc_type=kb）。
     */
    static String inferCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("前端") || lower.contains("vue") || lower.contains("react")
                || lower.contains("typescript") || lower.contains("webpack") || lower.contains("vite")
                || lower.contains("css")) {
            return "前端";
        }
        if (text.contains("算法") || lower.contains("leetcode") || text.contains("动态规划")
                || text.contains("数据结构") || text.contains("时间复杂度") || text.contains("空间复杂度")
                || text.contains("机器学习") || text.contains("深度学习") || lower.contains("numpy")
                || lower.contains("pandas") || lower.contains("pytorch") || lower.contains("tensorflow")) {
            return "算法";
        }
        if (text.contains("后端") || lower.contains("java") || lower.contains("spring")
                || lower.contains("mysql") || lower.contains("redis") || lower.contains("kafka")
                || text.contains("微服务") || lower.contains("jvm")) {
            return "后端";
        }
        if (text.contains("运维") || lower.contains("kubernetes") || lower.contains("docker")
                || lower.contains("jenkins") || text.contains("监控") || lower.contains("nginx")
                || lower.contains("prometheus") || lower.contains("k8s")) {
            return "运维";
        }
        if (text.contains("测试") || lower.contains("junit") || lower.contains("pytest")
                || lower.contains("jmeter") || text.contains("自动化") || text.contains("单测")) {
            return "测试";
        }
        return null;
    }
}
