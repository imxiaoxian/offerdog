package com.hanserdev.interview.model.vo.interview;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * 面试官语音播报的 WebSocket 消息。
 */
@Data
@Builder
public class AudioStreamMessage {

    public enum Type {
        START,
        CHUNK,
        COMPLETE,
        ERROR
    }

    private UUID sessionId;

    private int sequenceNumber;

    /**
     * START/CHUNK/COMPLETE/ERROR
     */
    private Type type;

    /**
     * 音频片段序号，仅在 CHUNK 事件下有值。
     */
    private Integer chunkIndex;

    /**
     * PCM Base64 字符串。
     */
    private String base64Audio;

    /**
     * DashScope 返回的音频格式名。
     */
    private String audioFormat;

    private String voice;

    /**
     * 对应的文本内容，仅在 START 事件传递，方便前端展示。
     */
    private String text;

    /**
     * 描述信息或错误信息。
     */
    private String message;

    private long timestamp;
}
