package com.hanserdev.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanserdev.interview.utils.ResumeTextExtractionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 使用 DeepSeek（经 {@link com.hanserdev.interview.config.ChatClientConfiguration}）从文档文本中抽取结构化 JSON。
 */
@Slf4j
@Service
public class InterviewStructuredExtractService {

    private static final int MAX_DOC_CHARS = 100_000;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public InterviewStructuredExtractService(
            @Qualifier("dashScopeChatClient") ChatClient dashScopeChatClient,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder) {
        this.chatClient = dashScopeChatClient;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    public <T> List<T> extractFromFileContent(
            byte[] fileContent,
            String fileName,
            Class<T> targetClass,
            String extractionInstruction) throws ExtractionException {

        log.info("结构化抽取(文件): {}", fileName);
        try {
            String docText = ResumeTextExtractionUtils.extractText(fileContent, fileName);
            return extractFromPlainText(docText, targetClass, extractionInstruction);
        } catch (IOException e) {
            throw new ExtractionException("文档转文本失败: " + describeThrowable(e), e);
        }
    }

    public <T> List<T> extractFromDocument(
            String documentUrl,
            Class<T> targetClass,
            String extractionInstruction) throws ExtractionException {

        log.info("结构化抽取(URL): {}", documentUrl);
        try {
            byte[] body = webClientBuilder.build()
                    .get()
                    .uri(URI.create(documentUrl))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofSeconds(90));
            if (body == null || body.length == 0) {
                throw new ExtractionException("下载文档失败：空内容");
            }
            String path = URI.create(documentUrl).getPath();
            String name = path != null && path.contains("/")
                    ? path.substring(path.lastIndexOf('/') + 1)
                    : "document.bin";
            return extractFromFileContent(body, name, targetClass, extractionInstruction);
        } catch (WebClientResponseException e) {
            throw new ExtractionException("下载文档失败 HTTP " + e.getStatusCode(), e);
        } catch (ExtractionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtractionException("下载或解析文档失败: " + describeThrowable(e), e);
        }
    }

    private <T> List<T> extractFromPlainText(String docText, Class<T> targetClass, String extractionInstruction)
            throws ExtractionException {
        if (docText.length() > MAX_DOC_CHARS) {
            log.warn("文档过长，已截断至 {} 字符", MAX_DOC_CHARS);
            docText = docText.substring(0, MAX_DOC_CHARS);
        }

        try {
            String jsonSchema = generateJsonSchema(targetClass);
            String userPrompt = buildPrompt(extractionInstruction, jsonSchema)
                    + "\n\n------- 文档正文 -------\n"
                    + docText;

            String raw = chatClient.prompt()
                    .system("你是一个专业的知识提取助手。请严格按照用户指定的 JSON Schema 输出**唯一一个** JSON 数组，"
                            + "不要包含 Markdown 代码块或其它说明文字。")
                    .user(userPrompt)
                    .call()
                    .content();

            if (raw == null || raw.isBlank()) {
                throw new ExtractionException("模型返回空内容");
            }

            String cleanJson = stripMarkdownJson(raw.trim());
            return readJsonListOrSingleObject(cleanJson, targetClass);
        } catch (ExtractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("结构化抽取失败", e);
            throw new ExtractionException("结构化抽取失败: " + describeThrowable(e), e);
        }
    }

    /** NPE 等异常的 getMessage() 常为 null，避免前端只看到「失败: null」。 */
    private static String describeThrowable(Throwable e) {
        if (e == null) {
            return "unknown";
        }
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName());
        Throwable c = e.getCause();
        int depth = 0;
        while (c != null && depth < 5) {
            String cm = c.getMessage();
            sb.append(" → ").append(c.getClass().getSimpleName());
            if (cm != null && !cm.isBlank()) {
                sb.append(": ").append(cm);
            }
            c = c.getCause();
            depth++;
        }
        return sb.toString();
    }

    /**
     * 优先按 JSON 数组解析；模型常误返回单个对象，则再解析为单条并包装成列表。
     */
    private <T> List<T> readJsonListOrSingleObject(String cleanJson, Class<T> targetClass)
            throws ExtractionException {
        String trimmed = cleanJson != null ? cleanJson.trim() : "";
        if ("null".equals(trimmed)) {
            throw new ExtractionException("模型返回 JSON 字面量 null，请重试或换一份简历文件");
        }
        try {
            List<T> list = objectMapper.readValue(trimmed,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, targetClass));
            if (list != null) {
                List<T> nonNull = new ArrayList<>();
                for (T item : list) {
                    if (item != null) {
                        nonNull.add(item);
                    }
                }
                if (nonNull.isEmpty()) {
                    throw new ExtractionException("模型返回的 JSON 数组为空或仅含 null，请重试");
                }
                return nonNull;
            }
        } catch (JsonProcessingException arrayError) {
            try {
                T single = objectMapper.readValue(trimmed, targetClass);
                if (single == null) {
                    throw new ExtractionException("模型解析结果为空对象，请重试");
                }
                return List.of(single);
            } catch (JsonProcessingException objectError) {
                String hint = arrayError.getOriginalMessage();
                if (hint == null || hint.isBlank()) {
                    hint = arrayError.getMessage();
                }
                if (hint == null || hint.isBlank()) {
                    hint = describeThrowable(arrayError);
                }
                throw new ExtractionException(
                        "模型返回无法解析为 JSON 数组或对象: " + hint,
                        arrayError);
            }
        } catch (ExtractionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtractionException("解析模型 JSON 失败: " + describeThrowable(e), e);
        }
        throw new ExtractionException("模型返回无法解析为 JSON 数组");
    }

    private static String stripMarkdownJson(String content) {
        String s = content;
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        s = s.trim();
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3).trim();
        }
        return s;
    }

    private String generateJsonSchema(Class<?> clazz) {
        StringBuilder schema = new StringBuilder();
        boolean isRecord = clazz.isRecord();

        if (isRecord) {
            schema.append("public record ").append(clazz.getSimpleName()).append("(\n");
            var recordComponents = clazz.getRecordComponents();
            for (int i = 0; i < recordComponents.length; i++) {
                var component = recordComponents[i];
                schema.append("  ").append(getSimpleTypeName(component.getType())).append(" ").append(component.getName());
                schema.append(", // ").append(getFieldDescription(component.getName()));
                if (i < recordComponents.length - 1) {
                    schema.append("\n");
                }
            }
            schema.append("\n) {}");
        } else {
            schema.append("public class ").append(clazz.getSimpleName()).append(" {\n");
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                schema.append("  private ").append(getSimpleTypeName(field.getType())).append(" ")
                        .append(field.getName()).append("; // ")
                        .append(getFieldDescription(field.getName())).append("\n");
            }
            schema.append("}");
        }
        return schema.toString();
    }

    private String getSimpleTypeName(Class<?> type) {
        if (type.isArray()) {
            return getSimpleTypeName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    private String getFieldDescription(String fieldName) {
        return switch (fieldName.toLowerCase(Locale.ROOT)) {
            case "content" -> "内容";
            case "answer" -> "答案";
            case "tips" -> "提示或思路";
            case "difficulty" -> "难度等级";
            case "tags" -> "标签数组";
            case "title" -> "标题";
            case "description" -> "描述";
            case "id" -> "唯一标识";
            case "name" -> "名称";
            default -> "字段说明";
        };
    }

    private String buildPrompt(String extractionInstruction, String jsonSchema) {
        return String.format(
                "%s请将结果整理成一个JSON数组，数组中的每个对象必须严格遵循以下结构定义：\n%s\n\n请直接输出JSON数组，不要包含任何解释性文字。",
                extractionInstruction,
                jsonSchema
        );
    }

    public static class ExtractionException extends Exception {
        public ExtractionException(String message) {
            super(message);
        }

        public ExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
