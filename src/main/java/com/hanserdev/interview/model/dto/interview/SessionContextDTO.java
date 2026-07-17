package com.hanserdev.interview.model.dto.interview;

import com.hanserdev.interview.domain.dataobject.ConversationMessageDO;
import com.hanserdev.interview.domain.dataobject.InterviewSessionDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 会话上下文DTO
 * 封装AI生成响应所需的完整上下文
 */
@Data
@Builder
public class SessionContextDTO {

    /**
     * 会话信息
     */
    private InterviewSessionDO session;

    /**
     * 对话消息列表
     */
    private List<ConversationMessageDO> messages;

    /**
     * 当前问题ID
     */
    private String currentQuestionId;
}

