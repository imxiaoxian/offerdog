package com.hanserdev.interview.model.vo.interview;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话消息响应 VO
 */
@Data
public class ConversationMessageRspVO {

    /**
     * 消息 ID
     */
    private UUID id;

    /**
     * 会话 ID
     */
    private UUID sessionId;

    /**
     * 消息角色（interviewer/candidate）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联题目 ID
     */
    private String relatedQuestionId;

    /**
     * 序号
     */
    private Integer sequenceNumber;

    /**
     * Token 数
     */
    private Integer tokenCount;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}
