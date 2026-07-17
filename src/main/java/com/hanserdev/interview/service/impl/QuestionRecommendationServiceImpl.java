package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.domain.dataobject.InterviewSessionDO;
import com.hanserdev.interview.domain.dataobject.InterviewTemplateDO;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.mapper.CategoryMapper;
import com.hanserdev.interview.domain.mapper.InterviewSessionMapper;
import com.hanserdev.interview.domain.mapper.InterviewTemplateMapper;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.vo.interview.ReportDetailRspVO;
import com.hanserdev.interview.model.vo.question.QuestionRecommendationRspVO;
import com.hanserdev.interview.model.vo.question.RecommendedQuestionVO;
import com.hanserdev.interview.service.InterviewReportService;
import com.hanserdev.interview.service.QuestionRecommendationService;
import com.hanserdev.interview.service.support.ReportWeakPointExtractor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 实现类
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 23:11
 */
@Service
@Slf4j
public class QuestionRecommendationServiceImpl implements QuestionRecommendationService {

    private static final Pattern ENGLISH_TOKEN = Pattern.compile("[a-zA-Z][a-zA-Z0-9]{2,}");

    /** 仅检索题库题目向量，避免与知识库 kb 文档混排后 question_id 全为空 */
    private static final Filter.Expression QUESTION_DOC_FILTER =
            new FilterExpressionBuilder().eq("doc_type", "question").build();

    @Resource
    private InterviewReportService reportService;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private InterviewSessionMapper interviewSessionMapper;

    @Resource
    private InterviewTemplateMapper interviewTemplateMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Value("${interview.recommend.similarity-threshold:0.32}")
    private double similarityThreshold;

    @Value("${interview.recommend.top-k-per-weak-point:10}")
    private int topKPerWeakPoint;

    /** 向量命中不足时，用题干关键词从 questions 表补足到此数量（上限仍受最终 20 条截断） */
    @Value("${interview.recommend.db-fallback-min-total:12}")
    private int dbFallbackMinTotal;

    @Override
    public QuestionRecommendationRspVO generateRecommendQuestions(UUID sessionId) {
        log.info("开始生成题目推荐: sessionId={}", sessionId);

        try {
            // 1. 获取面试报告
            ReportDetailRspVO report = reportService.getReportBySessionId(sessionId);
            JsonNode reportContent = report.getReportContent();

            // 2. 提取薄弱知识点
            List<String> weakPoints = ReportWeakPointExtractor.extractWeakPoints(reportContent);

            if (weakPoints.isEmpty()) {
                log.info("该面试报告没有发现薄弱点，无需推荐题目: sessionId={}", sessionId);
                return buildEmptyRecommendation(sessionId);
            }

            log.info("提取到薄弱知识点: {}", weakPoints);

            // 3. 从向量库 + 题库表（关键词 / 岗位范围）选题
            Map<Long, RecommendedQuestionVO> recommendedQuestions = searchRelevantQuestions(sessionId, weakPoints);

            // 4. 构建推荐结果
            QuestionRecommendationRspVO result = QuestionRecommendationRspVO.builder()
                    .sessionId(sessionId)
                    .weakPoints(weakPoints)
                    .recommendedQuestions(new ArrayList<>(recommendedQuestions.values()))
                    .reason(generateReason(weakPoints, recommendedQuestions.size()))
                    .generatedAt(LocalDateTime.now())
                    .build();

            log.info("题目推荐生成成功: sessionId={}, 推荐题目数={}", sessionId, recommendedQuestions.size());
            return result;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成题目推荐失败: sessionId={}", sessionId, e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 从向量库搜索相关题目（仅 doc_type=question），不足时用数据库题干关键词补足。
     */
    private Map<Long, RecommendedQuestionVO> searchRelevantQuestions(UUID sessionId, List<String> weakPoints) {
        Map<Long, RecommendedQuestionVO> uniqueQuestions = new LinkedHashMap<>();

        for (String weakPoint : weakPoints) {
            try {
                List<Document> similarDocs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(weakPoint)
                                .topK(topKPerWeakPoint)
                                .similarityThreshold(similarityThreshold)
                                .filterExpression(QUESTION_DOC_FILTER)
                                .build()
                );

                log.debug("薄弱点 '{}' 向量检索到 {} 条题库文档", weakPoint, similarDocs.size());

                for (Document doc : similarDocs) {
                    // 从 metadata 中获取题目ID
                    Object questionIdObj = doc.getMetadata().get("question_id");
                    if (questionIdObj == null) {
                        continue;
                    }

                    Long questionId = Long.parseLong(questionIdObj.toString());

                    // 去重：如果该题目已存在，更新相似度（取最高值）
                    if (uniqueQuestions.containsKey(questionId)) {
                        RecommendedQuestionVO existing = uniqueQuestions.get(questionId);
                        Double newScore = getScore(doc);
                        if (newScore > existing.getSimilarityScore()) {
                            existing.setSimilarityScore(newScore);
                            existing.setMatchedPoint(weakPoint);
                        }
                    } else {
                        // 新题目，创建VO
                        RecommendedQuestionVO questionVO = RecommendedQuestionVO.builder()
                                .id(questionId)
                                .matchedPoint(weakPoint)
                                .similarityScore(getScore(doc))
                                .build();

                        uniqueQuestions.put(questionId, questionVO);
                    }
                }
            } catch (Exception e) {
                log.warn("搜索薄弱点 '{}' 相关题目失败", weakPoint, e);
            }
        }

        hydrateQuestionDetails(uniqueQuestions);

        if (uniqueQuestions.size() < dbFallbackMinTotal) {
            int before = uniqueQuestions.size();
            mergeDatabaseKeywordFallback(weakPoints, uniqueQuestions);
            log.info("题库关键词/标签补足: 补足前 {} 条, 补足后 {} 条", before, uniqueQuestions.size());
        }

        if (uniqueQuestions.size() < dbFallbackMinTotal) {
            int before = uniqueQuestions.size();
            mergeCategoryScopedFallback(sessionId, weakPoints, uniqueQuestions);
            log.info("按面试模板岗位大类从题库补足: 补足前 {} 条, 补足后 {} 条", before, uniqueQuestions.size());
        }

        if (uniqueQuestions.isEmpty()) {
            mergeGlobalRecentFallback(weakPoints, uniqueQuestions);
            log.info("全库最新题目兜底: 当前 {} 条", uniqueQuestions.size());
        }

        if (uniqueQuestions.size() < 3) {
            int before = uniqueQuestions.size();
            mergeGlobalRecentFallback(weakPoints, uniqueQuestions);
            if (uniqueQuestions.size() > before) {
                log.info("推荐题不足 3 道，已用全库最新题补足: {} -> {} 条", before, uniqueQuestions.size());
            }
        }

        // 按相似度排序，取前20题
        return uniqueQuestions.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(
                        e2.getValue().getSimilarityScore(),
                        e1.getValue().getSimilarityScore()
                ))
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private void hydrateQuestionDetails(Map<Long, RecommendedQuestionVO> uniqueQuestions) {
        if (uniqueQuestions.isEmpty()) {
            return;
        }
        List<Long> questionIds = new ArrayList<>(uniqueQuestions.keySet());
        List<QuestionDO> questions = questionMapper.selectList(Wrappers.<QuestionDO>lambdaQuery()
                .in(QuestionDO::getId, questionIds));

        for (QuestionDO question : questions) {
            RecommendedQuestionVO vo = uniqueQuestions.get(question.getId());
            if (vo != null) {
                vo.setContent(question.getContent());
                vo.setDifficulty(question.getDifficulty() != null ? question.getDifficulty().name() : "medium");
                vo.setTags(question.getTags() != null ? Arrays.asList(question.getTags()) : List.of());
            }
        }

        uniqueQuestions.entrySet().removeIf(entry -> entry.getValue().getContent() == null);
    }

    /**
     * 从薄弱点拆出关键词，在 questions.content 上做 LIKE 匹配，不依赖向量是否已建索引。
     */
    private void mergeDatabaseKeywordFallback(List<String> weakPoints, Map<Long, RecommendedQuestionVO> uniqueQuestions) {
        List<String> tokens = extractSearchTokens(weakPoints);
        if (tokens.isEmpty()) {
            return;
        }
        int want = Math.max(0, dbFallbackMinTotal - uniqueQuestions.size());
        if (want == 0) {
            return;
        }

        Set<Long> existing = new HashSet<>(uniqueQuestions.keySet());
        int tokenUse = Math.min(tokens.size(), 18);
        LambdaQueryWrapper<QuestionDO> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(QuestionDO::getDeletedAt);
        if (!existing.isEmpty()) {
            wrapper.notIn(QuestionDO::getId, existing);
        }
        wrapper.and(w -> {
            for (int i = 0; i < tokenUse; i++) {
                String tok = tokens.get(i);
                String like = "%" + tok + "%";
                String clause =
                        "(content ILIKE {0} OR array_to_string(coalesce(tags, ARRAY[]::text[]), ',') ILIKE {0})";
                if (i == 0) {
                    w.apply(clause, like);
                } else {
                    w.or().apply(clause, like);
                }
            }
        });
        wrapper.last("LIMIT " + Math.min(120, want * 6));

        List<QuestionDO> rows = questionMapper.selectList(wrapper);
        double score = 0.41;
        for (QuestionDO row : rows) {
            if (uniqueQuestions.size() >= dbFallbackMinTotal) {
                break;
            }
            if (row.getId() == null || uniqueQuestions.containsKey(row.getId())) {
                continue;
            }
            String matched = matchWeakPointForQuestion(weakPoints, row.getContent());
            RecommendedQuestionVO vo = RecommendedQuestionVO.builder()
                    .id(row.getId())
                    .content(row.getContent())
                    .difficulty(row.getDifficulty() != null ? row.getDifficulty().name() : "medium")
                    .tags(row.getTags() != null ? Arrays.asList(row.getTags()) : List.of())
                    .matchedPoint(matched != null ? matched : weakPoints.get(0))
                    .similarityScore(score)
                    .build();
            uniqueQuestions.put(row.getId(), vo);
            score = Math.max(0.25, score - 0.008);
        }
    }

    private static List<String> extractSearchTokens(List<String> weakPoints) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        StringBuilder blob = new StringBuilder();
        for (String wp : weakPoints) {
            if (wp == null || wp.isBlank()) {
                continue;
            }
            String trimmed = wp.trim();
            blob.append(trimmed).append(' ');
            String normalized = trimmed.replace('（', '、').replace('）', '、').replace('/', '、');
            for (String part : normalized.split("[、，,。.；;（）()\\[\\]/|：:\\s]+")) {
                String t = part.trim();
                if (t.length() >= 2) {
                    set.add(t);
                }
            }
            if (trimmed.length() >= 4 && trimmed.length() <= 80) {
                set.add(trimmed);
            }
            Matcher m = ENGLISH_TOKEN.matcher(trimmed);
            while (m.find()) {
                set.add(m.group());
            }
        }
        addTechHintsIfPresent(blob.toString(), set);
        return new ArrayList<>(set);
    }

    /**
     * 报告薄弱点常见表述与题库题干/标签用词不完全一致，补充高频技术词以提高 LIKE 命中（尤其 tags 中含「集合」等）。
     */
    private static void addTechHintsIfPresent(String blob, Set<String> out) {
        if (blob == null || blob.isBlank()) {
            return;
        }
        String[][] hints = {
                {"集合", "集合框架", "Collection", "Map", "List", "Set", "Queue"},
                {"ArrayList", "LinkedList", "HashMap", "ConcurrentHashMap", "红黑树", "链表"},
                {"JVM", "垃圾回收", "GC", "线程", "并发", "锁", "Spring", "MySQL", "Redis", "索引"},
                {"Vue", "React", "TypeScript", "JavaScript", "Webpack", "Vite", "前端"},
                {"动态规划", "二叉树", "排序", "复杂度", "递归", "图"},
        };
        String lower = blob.toLowerCase(Locale.ROOT);
        for (String[] group : hints) {
            for (String h : group) {
                if (blob.contains(h) || lower.contains(h.toLowerCase(Locale.ROOT))) {
                    if (h.length() >= 2) {
                        out.add(h);
                    }
                }
            }
        }
    }

    private static String matchWeakPointForQuestion(List<String> weakPoints, String questionContent) {
        if (questionContent == null || questionContent.isBlank()) {
            return null;
        }
        String lower = questionContent.toLowerCase(Locale.ROOT);
        for (String wp : weakPoints) {
            if (wp == null || wp.isBlank()) {
                continue;
            }
            for (String part : wp.split("[、，,。.；;（）()\\s]+")) {
                String p = part.trim().toLowerCase(Locale.ROOT);
                if (p.length() >= 2 && lower.contains(p)) {
                    return wp;
                }
            }
        }
        return null;
    }

    private static String matchWeakPointForTags(List<String> weakPoints, String[] tags) {
        if (tags == null || tags.length == 0) {
            return null;
        }
        String joined = String.join(",", tags).toLowerCase(Locale.ROOT);
        for (String wp : weakPoints) {
            if (wp == null || wp.isBlank()) {
                continue;
            }
            for (String part : wp.split("[、，,。.；;（）()\\s]+")) {
                String p = part.trim().toLowerCase(Locale.ROOT);
                if (p.length() >= 2 && joined.contains(p)) {
                    return wp;
                }
            }
        }
        return null;
    }

    private List<Long> resolveJobCategoryIdsForSession(UUID sessionId) {
        InterviewSessionDO session = interviewSessionMapper.selectOne(Wrappers.<InterviewSessionDO>lambdaQuery()
                .eq(InterviewSessionDO::getSessionId, sessionId));
        if (session == null || session.getTemplateId() == null) {
            return List.of();
        }
        InterviewTemplateDO template = interviewTemplateMapper.selectOne(Wrappers.<InterviewTemplateDO>lambdaQuery()
                .eq(InterviewTemplateDO::getTemplateId, session.getTemplateId()));
        if (template == null) {
            return List.of();
        }
        return resolveJobCategoryIdsForTemplateCategory(template.getCategory());
    }

    /**
     * 模板 category 为「后端工程师」等，与二级岗位名（Java、Vue）不一致，按一级大类（后端/前端/算法…）圈定题库 category_id 范围。
     */
    private List<Long> resolveJobCategoryIdsForTemplateCategory(String templateCategory) {
        if (templateCategory == null || templateCategory.isBlank()) {
            return List.of();
        }
        String tc = templateCategory.trim();
        List<String> bucketNames = new ArrayList<>();
        if (tc.contains("全栈")) {
            bucketNames.add("前端");
            bucketNames.add("后端");
        } else {
            String bucketName = null;
            if (tc.contains("后端")) {
                bucketName = "后端";
            } else if (tc.contains("前端")) {
                bucketName = "前端";
            } else if (tc.contains("算法") || tc.contains("机器学习") || tc.contains("数据科学")) {
                bucketName = "算法";
            } else if (tc.contains("DevOps") || tc.contains("运维")) {
                bucketName = "DevOps";
            } else if (tc.contains("管理") || tc.contains("总监") || tc.contains("经理")) {
                bucketName = "技术管理";
            }
            if (bucketName != null) {
                bucketNames.add(bucketName);
            }
        }
        if (bucketNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        for (String bucketName : bucketNames) {
            CategoryDO root = categoryMapper.selectOne(Wrappers.<CategoryDO>lambdaQuery()
                    .eq(CategoryDO::getName, bucketName)
                    .eq(CategoryDO::getLevel, 1)
                    .isNull(CategoryDO::getParentId)
                    .isNull(CategoryDO::getDeletedAt));
            if (root == null || root.getId() == null) {
                continue;
            }
            List<CategoryDO> children = categoryMapper.selectList(Wrappers.<CategoryDO>lambdaQuery()
                    .eq(CategoryDO::getParentId, root.getId())
                    .eq(CategoryDO::getLevel, 2)
                    .isNull(CategoryDO::getDeletedAt));
            children.stream()
                    .map(CategoryDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(merged::add);
        }
        return new ArrayList<>(merged);
    }

    private void mergeCategoryScopedFallback(UUID sessionId, List<String> weakPoints,
            Map<Long, RecommendedQuestionVO> uniqueQuestions) {
        List<Long> categoryIds = resolveJobCategoryIdsForSession(sessionId);
        if (categoryIds.isEmpty()) {
            return;
        }
        int want = Math.max(0, dbFallbackMinTotal - uniqueQuestions.size());
        if (want == 0) {
            return;
        }
        LambdaQueryWrapper<QuestionDO> q = Wrappers.lambdaQuery();
        q.isNull(QuestionDO::getDeletedAt);
        q.in(QuestionDO::getCategoryId, categoryIds);
        if (!uniqueQuestions.isEmpty()) {
            q.notIn(QuestionDO::getId, uniqueQuestions.keySet());
        }
        q.orderByDesc(QuestionDO::getId);
        q.last("LIMIT " + Math.min(80, want * 4));
        List<QuestionDO> rows = questionMapper.selectList(q);
        double score = 0.37;
        for (QuestionDO row : rows) {
            if (uniqueQuestions.size() >= dbFallbackMinTotal) {
                break;
            }
            if (row.getId() == null || uniqueQuestions.containsKey(row.getId())) {
                continue;
            }
            String matched = matchWeakPointForQuestion(weakPoints, row.getContent());
            if (matched == null) {
                matched = matchWeakPointForTags(weakPoints, row.getTags());
            }
            RecommendedQuestionVO vo = RecommendedQuestionVO.builder()
                    .id(row.getId())
                    .content(row.getContent())
                    .difficulty(row.getDifficulty() != null ? row.getDifficulty().name() : "medium")
                    .tags(row.getTags() != null ? Arrays.asList(row.getTags()) : List.of())
                    .matchedPoint(matched != null ? matched : weakPoints.get(0))
                    .similarityScore(score)
                    .build();
            uniqueQuestions.put(row.getId(), vo);
            score = Math.max(0.22, score - 0.006);
        }
    }

    private void mergeGlobalRecentFallback(List<String> weakPoints, Map<Long, RecommendedQuestionVO> uniqueQuestions) {
        LambdaQueryWrapper<QuestionDO> q = Wrappers.lambdaQuery();
        q.isNull(QuestionDO::getDeletedAt);
        if (!uniqueQuestions.isEmpty()) {
            q.notIn(QuestionDO::getId, uniqueQuestions.keySet());
        }
        q.orderByDesc(QuestionDO::getId);
        q.last("LIMIT 20");
        List<QuestionDO> rows = questionMapper.selectList(q);
        double score = 0.3;
        for (QuestionDO row : rows) {
            if (row.getId() == null || uniqueQuestions.containsKey(row.getId())) {
                continue;
            }
            String matched = matchWeakPointForQuestion(weakPoints, row.getContent());
            if (matched == null) {
                matched = matchWeakPointForTags(weakPoints, row.getTags());
            }
            RecommendedQuestionVO vo = RecommendedQuestionVO.builder()
                    .id(row.getId())
                    .content(row.getContent())
                    .difficulty(row.getDifficulty() != null ? row.getDifficulty().name() : "medium")
                    .tags(row.getTags() != null ? Arrays.asList(row.getTags()) : List.of())
                    .matchedPoint(matched != null ? matched : weakPoints.get(0))
                    .similarityScore(score)
                    .build();
            uniqueQuestions.put(row.getId(), vo);
            score = Math.max(0.18, score - 0.01);
        }
    }

    /**
     * 获取文档相似度分数
     */
    private Double getScore(Document doc) {
        // Spring AI 的相似度分数在 metadata 中
        Object score = doc.getMetadata().get("distance");
        if (score instanceof Number) {
            // 距离越小越相似，转换为相似度分数（0-1）
            double distance = ((Number) score).doubleValue();
            return Math.max(0, 1 - distance);
        }
        return 0.5; // 默认分数
    }

    /**
     * 生成推荐原因
     */
    private String generateReason(List<String> weakPoints, int questionCount) {
        String area = weakPoints.stream().limit(3).collect(Collectors.joining("、"))
                + (weakPoints.size() > 3 ? "等" : "");
        String prefix = String.format(
                "根据您的面试表现分析，我们发现您在以下 %d 个方面有提升空间：%s。",
                weakPoints.size(),
                area);
        if (questionCount == 0) {
            return prefix + "当前题库中暂未匹配到合适的练习题目，请到「面试题库」按岗位浏览练习，或请管理员补充题目并执行题库向量化索引。";
        }
        return prefix + String.format(
                "为此，我们从题库中精选了 %d 道相关题目供您练习，帮助您巩固这些知识点。",
                questionCount);
    }

    /**
     * 构建空推荐结果（没有薄弱点）
     */
    private QuestionRecommendationRspVO buildEmptyRecommendation(UUID sessionId) {
        return QuestionRecommendationRspVO.builder()
                .sessionId(sessionId)
                .weakPoints(List.of())
                .recommendedQuestions(List.of())
                .reason("恭喜！您的面试表现非常出色，各方面都达到了预期水平，暂无特别需要加强的知识点。")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
