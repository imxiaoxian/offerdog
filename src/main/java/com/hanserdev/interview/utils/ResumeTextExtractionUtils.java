package com.hanserdev.interview.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 从简历/文档二进制中提取纯文本，供 DeepSeek 等非多模态文档 API 使用。
 */
@Slf4j
public final class ResumeTextExtractionUtils {

    private ResumeTextExtractionUtils() {
    }

    public static String extractText(byte[] fileContent, String fileName) throws IOException {
        if (fileContent == null || fileContent.length == 0) {
            throw new IOException("文件内容为空");
        }
        String name = fileName != null ? fileName : "document";
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return new String(fileContent, StandardCharsets.UTF_8).trim();
        }

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));

        if (lower.endsWith(".pdf")) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            return joinDocuments(pdfReader.get());
        }

        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        return joinDocuments(tikaReader.get());
    }

    /**
     * 面向「知识库向量化」的提取：纯文本/Markdown 直接按 UTF-8 读取，其余格式（含 PDF、Office 等）统一走 Apache Tika。
     */
    public static String extractTextWithTika(byte[] fileContent, String fileName) throws IOException {
        if (fileContent == null || fileContent.length == 0) {
            throw new IOException("文件内容为空");
        }
        String name = fileName != null ? fileName : "document";
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return new String(fileContent, StandardCharsets.UTF_8).trim();
        }

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        return joinDocuments(tikaReader.get());
    }

    private static String joinDocuments(List<Document> documents) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        for (Document doc : documents) {
            try {
                String content = doc.getText();
                if (content != null && !content.trim().isEmpty()) {
                    textBuilder.append(content).append('\n');
                }
            } catch (Exception e) {
                log.warn("提取文档片段失败: {}", e.getMessage());
            }
        }
        String result = textBuilder.toString().trim();
        if (result.isEmpty()) {
            throw new IOException("文档内容为空或无法提取文本");
        }
        return result;
    }
}
