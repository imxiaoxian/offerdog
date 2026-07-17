package com.hanserdev.interview.service;

/**
 * 从 classpath 知识库 Markdown 导入到 PgVector（metadata.doc_type = kb）。
 */
public interface InterviewKnowledgeBaseService {

    /**
     * 扫描 classpath 下 knowledge-base 目录中全部 Markdown，分块嵌入并写入向量库。
     *
     * @param replaceExisting 为 true 时先删除本次扫描将写入的文档 id（同路径重导覆盖）
     * @return 写入的文档块数量
     */
    int importFromClasspath(boolean replaceExisting);

    /**
     * 使用 Apache Tika（及纯文本直读）提取上传文件内容，分块后写入 PgVector，metadata 与 classpath 知识库一致（doc_type=kb）。
     *
     * @param fileContent       文件二进制
     * @param originalFilename  原始文件名（用于标题与稳定 id）
     * @param kbCategory        知识库分类：前端 / 后端 / 运维 / 测试，或 frontend、backend、devops、qa
     * @param replaceExisting   为 true 时先删除该文件对应的历史块再写入
     * @return 写入的文本块数量
     */
    int importFromUpload(byte[] fileContent, String originalFilename, String kbCategory, boolean replaceExisting);

    /**
     * 扫描本地 RAG 目录（默认项目下 {@code RAG/}，可通过配置 {@code interview.knowledge-base.rag-directory} 修改），
     * 对每个文件用 Tika 提取、分块后写入 PgVector。
     *
     * @param directoryOverride 非空时覆盖默认目录（相对路径相对于进程当前工作目录）
     * @param kbCategory        知识库分类
     * @param replacePerFile      每个文件写入前是否先删除该文件对应的历史向量块
     * @return 写入的文本块总数
     */
    int importFromRagDirectory(String directoryOverride, String kbCategory, boolean replacePerFile);
}
