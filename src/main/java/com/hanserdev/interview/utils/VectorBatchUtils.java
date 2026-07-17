package com.hanserdev.interview.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量化批处理工具类
 * 统一管理向量文档的批量添加逻辑
 *
 * @Author Zane
 * @CreateTime 2025/11/19 星期三 11:18
 */
@Component
@Slf4j
public class VectorBatchUtils {

    private static final int DEFAULT_BATCH_SIZE = 8;

    @Resource
    private PgVectorStore pgVectorStore;

    /**
     * 分批次添加文档到向量数据库
     * 硅基流动等 OpenAI 兼容嵌入接口对单请求条数有限制，保守分批
     *
     * @param documents 文档列表
     * @return 成功添加的文档数量
     */
    public int addDocumentsInBatches(List<Document> documents) {
        return addDocumentsInBatches(documents, DEFAULT_BATCH_SIZE);
    }

    /**
     * 分批次添加文档到向量数据库（自定义批次大小）
     *
     * @param documents 文档列表
     * @param batchSize 批次大小
     * @return 成功添加的文档数量
     */
    public int addDocumentsInBatches(List<Document> documents, int batchSize) {
        if (documents == null || documents.isEmpty()) {
            log.info("没有文档需要添加到向量数据库");
            return 0;
        }

        int totalDocuments = documents.size();

        if (totalDocuments <= batchSize) {
            log.info("文档数量 {} 小于等于批大小 {}，直接添加", totalDocuments, batchSize);
            pgVectorStore.add(documents);
            return totalDocuments;
        }

        int batches = (int) Math.ceil((double) totalDocuments / (double) batchSize);
        log.info("开始分批添加 {} 个文档到向量数据库，共 {} 批，每批最多 {} 个",
                totalDocuments, batches, batchSize);

        for (int i = 0; i < batches; i++) {
            int startIndex = i * batchSize;
            int endIndex = Math.min(startIndex + batchSize, totalDocuments);

            List<Document> batch = documents.subList(startIndex, endIndex);

            try {
                log.info("正在处理第 {}/{} 批，文档索引 {}-{}，批大小: {}",
                        i + 1, batches, startIndex, endIndex - 1, batch.size());

                pgVectorStore.add(batch);

                log.info("第 {}/{} 批处理成功", i + 1, batches);

            } catch (Exception e) {
                log.error("第 {}/{} 批处理失败，文档索引 {}-{}: {}",
                        i + 1, batches, startIndex, endIndex - 1, e.getMessage());
                throw new RuntimeException("向量数据库批处理失败: " + e.getMessage(), e);
            }
        }

        log.info("所有文档批处理完成，共添加 {} 个文档", totalDocuments);
        return totalDocuments;
    }
}