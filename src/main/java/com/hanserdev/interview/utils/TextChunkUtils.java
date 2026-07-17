package com.hanserdev.interview.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库/文档向量化前的固定长度分块（优先在段落边界截断）。
 */
public final class TextChunkUtils {

    /** 未读配置时的占位；实际以 {@code interview.knowledge-base.chunk-max-chars} 为准（需匹配嵌入模型 token 上限）。 */
    public static final int DEFAULT_CHUNK_CHARS = 450;

    private TextChunkUtils() {
    }

    public static List<String> splitChunks(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int para = text.lastIndexOf("\n\n", end);
                int minBreak = Math.max(40, maxChars / 5);
                if (para > start + minBreak) {
                    end = para + 2;
                }
            }
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) {
                out.add(part);
            }
            start = end;
        }
        return out;
    }
}
