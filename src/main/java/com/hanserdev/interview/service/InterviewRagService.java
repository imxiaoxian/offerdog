package com.hanserdev.interview.service;

/**
 * 面试对话 RAG：按当前题目主题从向量库检索知识库片段与题库题目（参考答案），注入提示词。
 */
public interface InterviewRagService {

    /**
     * 构建「知识库与题库参考」正文（无命中时返回简短说明，不抛异常）。
     */
    String buildRagContextBlock(String topic, String initialQuestion, String lastCandidateAnswer);
}
