package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.service.QuestionVectorService;
import com.hanserdev.interview.utils.VectorBatchUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 实现类
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 23:30
 */
@Service
@Slf4j
public class QuestionVectorServiceImpl implements QuestionVectorService {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private VectorBatchUtils vectorBatchUtils;

    @Override
    public void addQuestion(Long questionId) {
        log.info("开始索引题目到向量库: questionId={}", questionId);

        QuestionDO questionDO = questionMapper.selectById(questionId);

        if (questionDO == null || questionDO.getDeletedAt() != null) {
            throw new ApiException(ResponseCodeEnum.QUESTION_NOT_FOUND);
        }

        try {
            if (questionDO.getVectorAt() != null) {
                try {
                    UUID oldId = UUID.nameUUIDFromBytes(("question_" + questionId).getBytes(StandardCharsets.UTF_8));
                    pgVectorStore.delete(List.of(oldId.toString()));
                } catch (Exception ex) {
                    log.warn("覆盖索引前删除旧向量失败 questionId={}: {}", questionId, ex.toString());
                }
                questionMapper.update(new UpdateWrapper<QuestionDO>()
                        .eq("id", questionId)
                        .set("vector_at", null));
                questionDO = questionMapper.selectById(questionId);
            }

            // 将单个题目转换为向量文档
            Document document = convertToDocument(questionDO);
            if (document == null) {
                return;
            }

            pgVectorStore.add(List.of(document));
            questionMapper.update(new UpdateWrapper<QuestionDO>()
                    .eq("id", questionId)
                    .set("vector_at", LocalDateTime.now()));

            log.info("题目索引成功: questionId={}", questionId);
        } catch (Exception e) {
            log.error("题目索引失败: questionId={}", questionId, e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public Integer addQuestionBank(Long bankId) {
        log.info("开始批量索引题库: bankId={}", bankId);

        List<QuestionDO> questions = questionMapper.selectList(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getBankId, bankId)
                .isNull(QuestionDO::getDeletedAt));

        if (questions.isEmpty()) {
            log.warn("题库下无有效题目: bankId={}", bankId);
            return 0;
        }

        int ok = 0;
        for (QuestionDO q : questions) {
            try {
                addQuestion(q.getId());
                ok++;
            } catch (Exception e) {
                log.error("题库中单题索引失败 bankId={} questionId={}", bankId, q.getId(), e);
            }
        }
        log.info("题库批量索引完成: bankId={}, 尝试 {} 道, 成功 {} 道", bankId, questions.size(), ok);
        return ok;
    }

    @Override
    public void deleteQuestion(Long questionId) {
        log.info("从向量库删除题目: questionId={}", questionId);

        try {
            // 使用相同的 UUID 生成方式
            UUID documentId = UUID.nameUUIDFromBytes(("question_" + questionId).getBytes(StandardCharsets.UTF_8));
            List<String> ids = List.of(documentId.toString());

            pgVectorStore.delete(ids);
            questionMapper.update(new UpdateWrapper<QuestionDO>()
                    .eq("id", questionId)
                    .set("vector_at", null));

            log.info("题目从向量库删除成功: questionId={}", questionId);
        } catch (Exception e) {
            log.error("从向量库删除题目失败: questionId={}", questionId, e);
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public Integer rebuildVectorStore() {
        log.info("开始重建向量库索引");

        QueryWrapper<QuestionDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("deleted_at");

        List<QuestionDO> questions = questionMapper.selectList(wrapper);
        List<Document> documents = new ArrayList<>();

        if (!questions.isEmpty()) {
            for (QuestionDO question : questions) {
                Document document = convertToDocument(question);
                if (document != null) {
                    documents.add(document);
                }
                questionMapper.update(new UpdateWrapper<QuestionDO>()
                        .eq("id", question.getId())
                        .set("vector_at", LocalDateTime.now()));
            }
        }

        return vectorBatchUtils.addDocumentsInBatches(documents);
    }

    /**
     * 将题目转换为向量文档
     */
    private Document convertToDocument(QuestionDO question) {
        // 构建content
        StringBuilder contentBuilder = new StringBuilder();

        // 题目内容
        contentBuilder.append("题目: ").append(question.getContent()).append("\n\n");

        // 答案
        if (question.getAnswer() != null && !question.getAnswer().isEmpty()) {
            contentBuilder.append("参考答案: ").append(question.getAnswer()).append("\n\n");
        }

        // 提示
        if (question.getTips() != null && !question.getTips().isEmpty()) {
            contentBuilder.append("提示: ").append(question.getTips()).append("\n\n");
        }

        // 标签
        if (question.getTags() != null && question.getTags().length > 0) {
            contentBuilder.append("标签: ")
                    .append(String.join(", ", question.getTags()))
                    .append("\n");
        }

        String documentContent = contentBuilder.toString().trim();

        if (documentContent.isEmpty()) {
            log.warn("题目ID {} 内容为空，跳过向量化", question.getId());
            return null;
        }

        // 生成稳定的 UUID：基于命名空间和 question ID
        UUID documentId = UUID.nameUUIDFromBytes(("question_" + question.getId()).getBytes(StandardCharsets.UTF_8));

        String kbCategory = inferKbCategoryForQuestion(question);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("doc_type", "question");
        meta.put("kb_category", kbCategory);
        meta.put("question_id", question.getId().toString());
        meta.put("bank_id", question.getBankId().toString());
        meta.put("category_id", question.getCategoryId().toString());
        meta.put("difficulty", question.getDifficulty().name());
        meta.put("tags", question.getTags() != null ? Arrays.toString(question.getTags()) : "");
        meta.put("source", question.getSource().name());
        String shortTitle = question.getContent() == null ? "" : question.getContent().trim();
        if (shortTitle.length() > 80) {
            shortTitle = shortTitle.substring(0, 80) + "…";
        }
        meta.put("question_title", shortTitle);

        return new Document(documentId.toString(), documentContent, meta);
    }

    /**
     * 与知识库 RAG 使用同一套大类元数据（前端/后端/算法/运维/测试），便于过滤检索。
     */
    static String inferKbCategoryForQuestion(QuestionDO question) {
        String text = (question.getContent() != null ? question.getContent() : "")
                + " "
                + (question.getAnswer() != null ? question.getAnswer() : "");
        String cat = InterviewRagServiceImpl.inferCategory(text);
        return cat != null ? cat : "后端";
    }
}