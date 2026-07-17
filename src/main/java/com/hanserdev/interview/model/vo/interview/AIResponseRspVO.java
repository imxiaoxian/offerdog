package com.hanserdev.interview.model.vo.interview;

import com.hanserdev.interview.model.dto.interview.InterviewActionDTO;
import lombok.Builder;
import lombok.Data;

/**
 * AI响应VO
 */
@Data
@Builder
public class AIResponseRspVO {

    /**
     * AI响应内容
     */
    private String content;

    /**
     * 动作类型
     */
    private InterviewActionDTO.ActionType action;

    /**
     * 当前问题索引
     */
    private Integer currentQuestionIndex;

    /**
     * 面试是否完成
     */
    private Boolean completed;

    /**
     * 序列号
     */
    private Integer sequenceNumber;
}

