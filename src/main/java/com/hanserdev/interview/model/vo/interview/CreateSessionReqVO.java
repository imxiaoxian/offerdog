package com.hanserdev.interview.model.vo.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 创建面试会话请求VO
 */
@Data
public class CreateSessionReqVO {

    /**
     * 面试模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private UUID templateId;

    /**
     * 用户ID（可选，从session中获取）
     */
    private Long userId;
}

