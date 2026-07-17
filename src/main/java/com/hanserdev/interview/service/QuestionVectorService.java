package com.hanserdev.interview.service;

/**
 * 题目向量化
 *
 * @Author Zane
 * @CreateTime 2025/11/18 星期二 23:23
 */
public interface QuestionVectorService {

    /**
     * 将单个题目导入向量数据库
     *
     * @param questionId 题目ID
     */
    void addQuestion(Long questionId);

    /**
     * 将题库的所有题目导入向量数据库
     *
     * @param bankId 题库ID
     * @return 导入的题目数量
     */
    Integer addQuestionBank(Long bankId);

    /**
     * 从向量数据库中删除单个题目
     *
     * @param questionId 题目ID
     */
    void deleteQuestion(Long questionId);

    /**
     * 重建向量数据库
     *
     * @return 重建的题目数量
     */
    Integer rebuildVectorStore();
}

