package com.hanserdev.interview.model.vo.interview;

import lombok.Builder;
import lombok.Data;

/**
 * 来自内置知识库向量检索的片段。
 */
@Data
@Builder
public class KnowledgeSnippetRspVO {

    private String title;
    /** 知识库大类：前端/后端/算法/运维/测试 */
    private String kbCategory;
    private String excerpt;
    /** 逻辑来源标识（如 classpath URL 或 rag 路径） */
    private String sourceHint;
    private String matchedWeakPoint;
    private Double relevanceScore;
}
