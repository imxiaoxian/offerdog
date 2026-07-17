package com.hanserdev.interview.model.vo.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 提交回答请求VO
 */
@Data
public class SubmitAnswerReqVO {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private UUID sessionId;

    /**
     * 问题ID
     */
    @NotBlank(message = "问题ID不能为空")
    private String questionId;

    /**
     * 回答内容
     */
    @NotBlank(message = "回答内容不能为空")
    private String answer;
}

