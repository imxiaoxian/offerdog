package com.hanserdev.interview.service.impl;

import com.hanserdev.interview.service.InterviewKnowledgeBaseService;
import com.hanserdev.interview.utils.KnowledgeBaseChunkUtils;
import com.hanserdev.interview.utils.ResumeTextExtractionUtils;
import com.hanserdev.interview.utils.TextChunkUtils;
import com.hanserdev.interview.utils.VectorBatchUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 知识库目录与分类：knowledge-base 下 frontend、backend、algorithm、devops、qa 对应 前端、后端、算法、运维、测试。
 */
@Slf4j
@Service
public class InterviewKnowledgeBaseServiceImpl implements InterviewKnowledgeBaseService {

    private static final String PATTERN = "classpath:knowledge-base/**/*.md";
    private static final Set<String> KB_CATEGORY_CN = Set.of("前端", "后端", "算法", "运维", "测试");

    private static final Map<String, String> CATEGORY_BY_FOLDER = new LinkedHashMap<>();

    static {
        CATEGORY_BY_FOLDER.put("frontend", "前端");
        CATEGORY_BY_FOLDER.put("backend", "后端");
        CATEGORY_BY_FOLDER.put("algorithm", "算法");
        CATEGORY_BY_FOLDER.put("devops", "运维");
        CATEGORY_BY_FOLDER.put("qa", "测试");
    }

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private VectorBatchUtils vectorBatchUtils;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Value("${interview.knowledge-base.rag-directory:RAG}")
    private String ragDirectoryDefault;

    @Value("${interview.knowledge-base.chunk-max-chars:520}")
    private int chunkMaxChars;

    @Value("${interview.knowledge-base.chunk-min-chars:120}")
    private int chunkMinChars;

    @Value("${interview.knowledge-base.chunk-overlap-chars:72}")
    private int chunkOverlapChars;

    @Value("${interview.knowledge-base.chunk-strategy:recursive-semantic}")
    private String chunkStrategyRaw;

    @Value("${interview.knowledge-base.semantic-merge-threshold:0.78}")
    private double semanticMergeThreshold;

    @Value("${interview.knowledge-base.semantic-embed-batch-size:16}")
    private int semanticEmbedBatchSize;

    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String pgVectorSchema;

    @Value("${spring.ai.vectorstore.pgvector.table-name:interview_vectorstore}")
    private String pgVectorTable;

    private int effectiveChunkChars() {
        int n = chunkMaxChars > 0 ? chunkMaxChars : TextChunkUtils.DEFAULT_CHUNK_CHARS;
        return Math.min(Math.max(n, 128), 8000);
    }

    private KnowledgeBaseChunkUtils.Strategy resolveChunkStrategy() {
        String s = chunkStrategyRaw == null ? "" : chunkStrategyRaw.trim().toLowerCase();
        if ("legacy".equals(s) || "fixed".equals(s) || "old".equals(s)) {
            return KnowledgeBaseChunkUtils.Strategy.LEGACY;
        }
        return KnowledgeBaseChunkUtils.Strategy.RECURSIVE_SEMANTIC;
    }

    private List<String> chunkText(String raw) {
        KnowledgeBaseChunkUtils.Strategy st = resolveChunkStrategy();
        EmbeddingModel em = embeddingModelProvider.getIfAvailable();
        if (st == KnowledgeBaseChunkUtils.Strategy.RECURSIVE_SEMANTIC && em == null) {
            log.warn("chunk-strategy=recursive-semantic 但未注入 EmbeddingModel，回退为 legacy 分块");
            st = KnowledgeBaseChunkUtils.Strategy.LEGACY;
        }
        return KnowledgeBaseChunkUtils.chunk(
                raw,
                effectiveChunkChars(),
                Math.max(0, chunkMinChars),
                Math.max(0, chunkOverlapChars),
                st,
                st == KnowledgeBaseChunkUtils.Strategy.RECURSIVE_SEMANTIC ? em : null,
                semanticMergeThreshold,
                semanticEmbedBatchSize);
    }

    private String qualifiedVectorTable() {
        return "\"" + pgVectorSchema.replace("\"", "") + "\".\"" + pgVectorTable.replace("\"", "") + "\"";
    }

    /**
     * 删除 classpath 打包知识库（不含 upload://、rag://），用于 replace 真正覆盖（避免分块策略变化后遗留旧块）。
     */
    private int deleteClasspathPackagedKbVectors() {
        String sql = "DELETE FROM " + qualifiedVectorTable()
                + " WHERE COALESCE(metadata::jsonb->>'doc_type','') = 'kb'"
                + " AND COALESCE(metadata::jsonb->>'kb_source','') NOT LIKE 'upload://%'"
                + " AND COALESCE(metadata::jsonb->>'kb_source','') NOT LIKE 'rag://%'";
        int n = jdbcTemplate.update(sql);
        log.info("已按元数据删除 classpath 知识库向量 {} 条（doc_type=kb，排除 upload/rag）", n);
        return n;
    }

    /** 按精确 kb_source 删除（单文件上传 / 单 RAG 文件重导） */
    private int deleteKbVectorsBySourceExact(String kbSource) {
        if (kbSource == null || kbSource.isBlank()) {
            return 0;
        }
        String sql = "DELETE FROM " + qualifiedVectorTable()
                + " WHERE COALESCE(metadata::jsonb->>'doc_type','') = 'kb'"
                + " AND COALESCE(metadata::jsonb->>'kb_source','') = ?";
        int n = jdbcTemplate.update(sql, kbSource);
        if (n > 0) {
            log.info("已删除 kb_source={} 的向量 {} 条", kbSource, n);
        }
        return n;
    }

    @Override
    public int importFromClasspath(boolean replaceExisting) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] markdownFiles;
        try {
            markdownFiles = resolver.getResources(PATTERN);
        } catch (Exception e) {
            throw new IllegalStateException("扫描知识库资源失败: " + e.getMessage(), e);
        }
        if (markdownFiles.length == 0) {
            log.warn("未找到知识库文件: {}", PATTERN);
            return 0;
        }

        if (replaceExisting) {
            deleteClasspathPackagedKbVectors();
        }

        List<Document> documents = new ArrayList<>();

        for (org.springframework.core.io.Resource res : markdownFiles) {
            try {
                String path = res.getURL().toString().replace('\\', '/');
                String category = resolveCategory(path);
                String raw = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
                String title = extractTitle(raw, res.getFilename());
                List<String> chunks = chunkText(raw.trim());
                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    if (chunk.isBlank()) {
                        continue;
                    }
                    String stableKey = "kb_" + category + "_" + path + "_" + i;
                    String id = UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
                    documents.add(new Document(
                            id,
                            chunk,
                            Map.of(
                                    "doc_type", "kb",
                                    "kb_category", category,
                                    "kb_title", title,
                                    "kb_source", path,
                                    "chunk_index", String.valueOf(i)
                            )
                    ));
                }
            } catch (Exception e) {
                log.error("处理知识库文件失败: {}", res, e);
                throw new IllegalStateException("读取知识库失败: " + res.getFilename(), e);
            }
        }

        if (documents.isEmpty()) {
            return 0;
        }

        int added = vectorBatchUtils.addDocumentsInBatches(documents);
        log.info("知识库导入完成，共 {} 块", added);
        return added;
    }

    static String resolveCategory(String path) {
        String p = path.replace('\\', '/');
        for (Map.Entry<String, String> e : CATEGORY_BY_FOLDER.entrySet()) {
            if (p.contains("/knowledge-base/" + e.getKey() + "/")) {
                return e.getValue();
            }
        }
        log.warn("无法从路径推断分类，默认「后端」: {}", path);
        return "后端";
    }

    private static String extractTitle(String markdown, String filename) {
        for (String line : markdown.split("\r?\n")) {
            String t = line.strip();
            if (t.startsWith("# ")) {
                return t.substring(2).trim();
            }
        }
        if (filename != null && filename.endsWith(".md")) {
            return filename.substring(0, filename.length() - 3);
        }
        return "知识库";
    }

    @Override
    public int importFromUpload(byte[] fileContent, String originalFilename, String kbCategory, boolean replaceExisting) {
        String category = normalizeKbCategory(kbCategory);
        String name = originalFilename != null && !originalFilename.isBlank() ? originalFilename.trim() : "upload.bin";
        String safeName = sanitizePathSegment(name);
        String title = titleFromFilename(name);
        String virtualPath = "upload://" + category + "/" + safeName;
        return ingestTikaToKbVectors(fileContent, name, category, replaceExisting, "upload", safeName, virtualPath, title);
    }

    @Override
    public int importFromRagDirectory(String directoryOverride, String kbCategory, boolean replacePerFile) {
        String category = normalizeKbCategory(kbCategory);
        Path base = resolveRagBasePath(directoryOverride);
        if (!Files.isDirectory(base)) {
            throw new IllegalStateException("RAG 目录不存在或不是文件夹: " + base.toAbsolutePath());
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> !isIgnoredRagFile(p))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            throw new IllegalStateException("扫描 RAG 目录失败: " + e.getMessage(), e);
        }
        if (files.isEmpty()) {
            log.warn("RAG 目录下没有可导入文件: {}", base.toAbsolutePath());
            return 0;
        }
        int total = 0;
        for (Path file : files) {
            Path relative = base.relativize(file);
            String relativeSlash = relative.toString().replace('\\', '/');
            String safeLogical = sanitizePathSegment(relativeSlash);
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new IllegalStateException("读取文件失败: " + file, e);
            }
            String fileName = file.getFileName().toString();
            String title = titleFromFilename(fileName);
            String virtualPath = "rag://" + category + "/" + relativeSlash;
            int n = ingestTikaToKbVectors(bytes, fileName, category, replacePerFile, "rag", safeLogical, virtualPath, title);
            log.info("RAG 文件已向量化 {} 块: {}", n, relativeSlash);
            total += n;
        }
        log.info("RAG 目录导入完成，共 {} 个文件、{} 个文本块", files.size(), total);
        return total;
    }

    private Path resolveRagBasePath(String directoryOverride) {
        String dir = directoryOverride != null && !directoryOverride.isBlank()
                ? directoryOverride.trim()
                : ragDirectoryDefault;
        Path p = Paths.get(dir);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir", ".")).resolve(p).normalize();
        }
        return p.normalize();
    }

    private static boolean isIgnoredRagFile(Path p) {
        String n = p.getFileName().toString();
        if (n.startsWith(".")) {
            return true;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        return lower.endsWith(".tmp") || "thumbs.db".equals(lower) || ".ds_store".equals(lower);
    }

    /**
     * @param idNamespace   用于稳定 id，如 upload / rag
     * @param safeLogical   已清理的逻辑键（上传为文件名，RAG 为相对路径）
     */
    private int ingestTikaToKbVectors(byte[] fileContent, String tikaFileName, String category,
            boolean replaceExisting, String idNamespace, String safeLogical, String kbSource, String kbTitle) {
        String raw;
        try {
            raw = ResumeTextExtractionUtils.extractTextWithTika(fileContent, tikaFileName);
        } catch (Exception e) {
            throw new IllegalStateException("Tika/文本提取失败 [" + tikaFileName + "]: " + e.getMessage(), e);
        }
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("提取结果为空，无法向量化: " + tikaFileName);
        }

        if (replaceExisting) {
            deleteKbVectorsBySourceExact(kbSource);
        }

        List<String> chunks = chunkText(raw.trim());
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (chunk.isBlank()) {
                continue;
            }
            String stableKey = "kb_" + idNamespace + "_" + category + "_" + safeLogical + "_" + i;
            String id = UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
            documents.add(new Document(
                    id,
                    chunk,
                    Map.of(
                            "doc_type", "kb",
                            "kb_category", category,
                            "kb_title", kbTitle,
                            "kb_source", kbSource,
                            "chunk_index", String.valueOf(i)
                    )
            ));
        }

        if (documents.isEmpty()) {
            return 0;
        }

        return vectorBatchUtils.addDocumentsInBatches(documents);
    }

    private static String sanitizePathSegment(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\u4e00-\\u9fa5/-]", "_").replace("/", "_");
    }

    private static String normalizeKbCategory(String kbCategory) {
        if (kbCategory == null || kbCategory.isBlank()) {
            return "后端";
        }
        String t = kbCategory.trim();
        if (KB_CATEGORY_CN.contains(t)) {
            return t;
        }
        String key = t.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : CATEGORY_BY_FOLDER.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        throw new IllegalArgumentException("无效的知识库分类，请使用：前端、后端、算法、运维、测试（或 frontend/backend/algorithm/devops/qa）");
    }

    private static String titleFromFilename(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String base = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            return base.substring(0, dot);
        }
        return base.isEmpty() ? "上传文档" : base;
    }
}
