package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.service.InterviewKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 知识库（前端/后端/算法/运维/测试）导入与重建。
 */
@RestController
@RequestMapping("/admin/knowledge-base")
@Validated
@RequiredArgsConstructor
public class KnowledgeBaseAdminController {

    private final InterviewKnowledgeBaseService interviewKnowledgeBaseService;

    /**
     * 从 classpath knowledge-base 目录导入 Markdown；replace=true 时先删旧块再写入。
     */
    @PostMapping("/import")
    @ApiOperationLog(description = "导入知识库到向量库")
    public Response<Integer> importKnowledgeBase(
            @RequestParam(defaultValue = "true") boolean replace) {
        int n = interviewKnowledgeBaseService.importFromClasspath(replace);
        Response<Integer> response = Response.success(n);
        response.setMessage(String.format("已导入知识库 %d 个文本块", n));
        return response;
    }

    /**
     * 上传文件：Apache Tika 提取文本 → 分块 → 嵌入并写入 PgVector（与 classpath 知识库同一 doc_type=kb，供 RAG 使用）。
     */
    @PostMapping("/upload")
    @ApiOperationLog(description = "上传文档 Tika 提取并向量化")
    public Response<Integer> uploadAndVectorize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "kbCategory", defaultValue = "后端") String kbCategory,
            @RequestParam(defaultValue = "true") boolean replace) {
        if (file == null || file.isEmpty()) {
            return Response.fail("请上传非空文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            return Response.fail("读取上传文件失败");
        }
        String original = file.getOriginalFilename();
        int n = interviewKnowledgeBaseService.importFromUpload(bytes, original, kbCategory, replace);
        Response<Integer> response = Response.success(n);
        response.setMessage(String.format("已向量化 %d 个文本块（%s）", n, original != null ? original : "upload"));
        return response;
    }

    /**
     * 扫描本地 RAG 目录（默认 {@code RAG/}，相对当前工作目录；可用配置 {@code interview.knowledge-base.rag-directory} 或参数 {@code directory} 覆盖），
     * 将目录内全部文件 Tika 提取后分块写入向量库。
     */
    @PostMapping("/import-rag")
    @ApiOperationLog(description = "RAG 目录文件向量化入库")
    public Response<Integer> importRagDirectory(
            @RequestParam(value = "directory", required = false) String directory,
            @RequestParam(value = "kbCategory", defaultValue = "后端") String kbCategory,
            @RequestParam(defaultValue = "true") boolean replace) {
        int n = interviewKnowledgeBaseService.importFromRagDirectory(directory, kbCategory, replace);
        Response<Integer> response = Response.success(n);
        response.setMessage(String.format("RAG 目录已向量化 %d 个文本块", n));
        return response;
    }
}
