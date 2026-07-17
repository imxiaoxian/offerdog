package com.hanserdev.interview.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库 RAG 分块：递归（Markdown 结构 + 多级分隔符）与可选语义合并（相邻块向量余弦相似度）。
 */
@Slf4j
public final class KnowledgeBaseChunkUtils {

    private static final Pattern MARKDOWN_HEADER_START = Pattern.compile("(?m)^#{1,6}\\s+");

    /**
     * 与 LangChain RecursiveCharacterTextSplitter 类似：优先按大块边界切，再递归细分。
     */
    private static final List<String> DEFAULT_SEPARATORS = List.of(
            "\n\n",
            "\n",
            "。",
            "！",
            "？",
            ". ",
            "! ",
            "? ",
            "；",
            "; ",
            "，",
            ", ",
            " "
    );

    private KnowledgeBaseChunkUtils() {
    }

    public enum Strategy {
        /** 仅固定窗口 + 段落偏好（原 {@link TextChunkUtils}） */
        LEGACY,
        /** Markdown 节 + 递归字符切分 + 可选重叠 + 可选语义合并 */
        RECURSIVE_SEMANTIC
    }

    /**
     * @param text              全文
     * @param maxChars          单块最大字符
     * @param minChars          过小则与相邻合并（非嵌入）
     * @param overlapChars      块间重叠（前缀承接上文）
     * @param strategy          策略
     * @param embeddingModel    语义合并时必填；为 null 时跳过语义步
     * @param semanticThreshold 相邻原子块余弦相似度 ≥ 此值且总长不超限时合并（约 0.72–0.85）
     * @param embedBatchSize    批量嵌入大小
     */
    public static List<String> chunk(
            String text,
            int maxChars,
            int minChars,
            int overlapChars,
            Strategy strategy,
            EmbeddingModel embeddingModel,
            double semanticThreshold,
            int embedBatchSize) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.trim();
        int cap = Math.min(Math.max(maxChars, 128), 8000);
        int min = Math.max(0, Math.min(minChars, cap));
        int ov = Math.max(0, Math.min(overlapChars, cap / 2));

        List<String> atomic;
        if (strategy == Strategy.LEGACY) {
            atomic = new ArrayList<>(TextChunkUtils.splitChunks(normalized, cap));
        } else {
            atomic = new ArrayList<>();
            for (String section : splitMarkdownSections(normalized)) {
                if (section.isBlank()) {
                    continue;
                }
                List<String> parts = recursiveSplitCharacters(section, cap, new ArrayList<>(DEFAULT_SEPARATORS));
                atomic.addAll(parts);
            }
            if (atomic.isEmpty()) {
                atomic.add(normalized);
            }
        }

        atomic.removeIf(String::isBlank);
        if (atomic.isEmpty()) {
            return List.of();
        }

        List<String> mergedSmall = mergeUndersizedNeighbors(atomic, min, cap);
        List<String> semantic = mergedSmall;
        if (strategy == Strategy.RECURSIVE_SEMANTIC
                && embeddingModel != null
                && mergedSmall.size() > 1
                && semanticThreshold > 0
                && semanticThreshold <= 1.0) {
            semantic = mergeByAdjacentEmbeddingSimilarity(
                    mergedSmall, embeddingModel, cap, semanticThreshold, Math.max(4, embedBatchSize));
        }

        List<String> withOverlap = applyOverlap(semantic, ov);
        // 嵌入接口常见 512 tokens 上限；中文保守按字符再切一刀，避免 413
        int embedSafe = Math.min(cap, Math.max(200, cap - Math.max(ov, 32)));
        return splitOversized(withOverlap, embedSafe);
    }

    /** 将超长块再切分，保证单条不超过 embedSafe（供 BGE 类等 512 token 模型） */
    static List<String> splitOversized(List<String> chunks, int embedSafe) {
        if (chunks.isEmpty() || embedSafe <= 0) {
            return new ArrayList<>(chunks);
        }
        List<String> out = new ArrayList<>();
        for (String c : chunks) {
            if (c == null || c.isBlank()) {
                continue;
            }
            if (c.length() <= embedSafe) {
                out.add(c);
            } else {
                out.addAll(TextChunkUtils.splitChunks(c, embedSafe));
            }
        }
        return out;
    }

    /** 按 Markdown 标题切分为节，每节保留标题行 */
    static List<String> splitMarkdownSections(String md) {
        Matcher m = MARKDOWN_HEADER_START.matcher(md);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
        }
        if (starts.isEmpty()) {
            return List.of(md);
        }
        List<String> sections = new ArrayList<>();
        if (starts.get(0) > 0) {
            String pre = md.substring(0, starts.get(0)).trim();
            if (!pre.isEmpty()) {
                sections.add(pre);
            }
        }
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = i + 1 < starts.size() ? starts.get(i + 1) : md.length();
            String sec = md.substring(from, to).trim();
            if (!sec.isEmpty()) {
                sections.add(sec);
            }
        }
        return sections.isEmpty() ? List.of(md) : sections;
    }

    static List<String> recursiveSplitCharacters(String text, int chunkSize, List<String> separators) {
        if (text.length() <= chunkSize) {
            return new ArrayList<>(List.of(text));
        }
        if (separators.isEmpty()) {
            return hardSplitByLength(text, chunkSize);
        }

        int chosenIdx = -1;
        String chosenSep = null;
        for (int i = 0; i < separators.size(); i++) {
            String sep = separators.get(i);
            if (sep.isEmpty()) {
                chosenIdx = i;
                chosenSep = sep;
                break;
            }
            if (text.contains(sep)) {
                chosenIdx = i;
                chosenSep = sep;
                break;
            }
        }
        if (chosenSep == null) {
            return hardSplitByLength(text, chunkSize);
        }

        List<String> nextSeparators = chosenIdx + 1 < separators.size()
                ? separators.subList(chosenIdx + 1, separators.size())
                : List.of();

        if (chosenSep.isEmpty()) {
            return hardSplitByLength(text, chunkSize);
        }

        String[] rawSplits = text.split(Pattern.quote(chosenSep), -1);
        List<String> subChunks = new ArrayList<>();
        for (String part : rawSplits) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.length() <= chunkSize) {
                subChunks.add(part);
            } else {
                subChunks.addAll(recursiveSplitCharacters(part, chunkSize, new ArrayList<>(nextSeparators)));
            }
        }
        return mergeSplitsWithSeparator(subChunks, chosenSep, chunkSize);
    }

    private static List<String> mergeSplitsWithSeparator(List<String> splits, String separator, int chunkSize) {
        if (splits.isEmpty()) {
            return List.of();
        }
        List<String> docs = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int total = 0;
        for (String d : splits) {
            int add = d.length() + (current.isEmpty() ? 0 : separator.length());
            if (total + add > chunkSize && !current.isEmpty()) {
                docs.add(String.join(separator, current));
                current.clear();
                total = 0;
                add = d.length();
            }
            current.add(d);
            total += add;
        }
        if (!current.isEmpty()) {
            docs.add(String.join(separator, current));
        }
        return docs;
    }

    private static List<String> hardSplitByLength(String text, int chunkSize) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) {
                out.add(part);
            }
            start = end;
        }
        return out.isEmpty() ? List.of(text) : out;
    }

    static List<String> mergeUndersizedNeighbors(List<String> chunks, int minChars, int maxChars) {
        if (minChars <= 0 || chunks.size() <= 1) {
            return new ArrayList<>(chunks);
        }
        List<String> work = new ArrayList<>(chunks);
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < work.size() * 4) {
            changed = false;
            for (int i = 0; i < work.size(); i++) {
                String c = work.get(i);
                if (c.length() >= minChars || work.size() <= 1) {
                    continue;
                }
                if (i > 0 && work.get(i - 1).length() + 2 + c.length() <= maxChars) {
                    work.set(i - 1, work.get(i - 1) + "\n\n" + c);
                    work.remove(i);
                    changed = true;
                    break;
                }
                if (i + 1 < work.size() && c.length() + 2 + work.get(i + 1).length() <= maxChars) {
                    work.set(i, c + "\n\n" + work.get(i + 1));
                    work.remove(i + 1);
                    changed = true;
                    break;
                }
            }
        }
        return work;
    }

    static List<String> mergeByAdjacentEmbeddingSimilarity(
            List<String> chunks,
            EmbeddingModel model,
            int maxChars,
            double threshold,
            int batchSize) {
        try {
            List<float[]> embeddings = embedAllInBatches(model, chunks, batchSize);
            if (embeddings.size() != chunks.size()) {
                log.warn("嵌入条数与分块不一致，跳过语义合并");
                return chunks;
            }
            List<String> out = new ArrayList<>();
            int i = 0;
            while (i < chunks.size()) {
                int j = i;
                StringBuilder acc = new StringBuilder(chunks.get(i));
                while (j + 1 < chunks.size()
                        && acc.length() + 2 + chunks.get(j + 1).length() <= maxChars
                        && cosineSimilarity(embeddings.get(j), embeddings.get(j + 1)) >= threshold) {
                    acc.append("\n\n").append(chunks.get(j + 1));
                    j++;
                }
                out.add(acc.toString().trim());
                i = j + 1;
            }
            return out;
        } catch (Exception e) {
            log.warn("语义合并失败，使用递归分块结果: {}", e.toString());
            return chunks;
        }
    }

    private static List<float[]> embedAllInBatches(EmbeddingModel model, List<String> chunks, int batchSize) {
        List<float[]> all = new ArrayList<>();
        int bs = Math.max(1, batchSize);
        for (int from = 0; from < chunks.size(); from += bs) {
            int to = Math.min(from + bs, chunks.size());
            List<String> sub = chunks.subList(from, to);
            List<float[]> part = model.embed(sub);
            all.addAll(part);
        }
        return all;
    }

    static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        double d = Math.sqrt(na) * Math.sqrt(nb);
        return d < 1e-9 ? 0 : dot / d;
    }

    static List<String> applyOverlap(List<String> chunks, int overlapChars) {
        if (overlapChars <= 0 || chunks.size() <= 1) {
            return new ArrayList<>(chunks);
        }
        List<String> out = new ArrayList<>();
        out.add(chunks.get(0));
        for (int i = 1; i < chunks.size(); i++) {
            String prev = chunks.get(i - 1);
            String cur = chunks.get(i);
            if (prev.length() <= overlapChars) {
                out.add(prev + "\n" + cur);
            } else {
                String prefix = prev.substring(prev.length() - overlapChars);
                out.add(prefix + "\n" + cur);
            }
        }
        return out;
    }
}
