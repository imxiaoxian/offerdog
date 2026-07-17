package com.hanserdev.interview.model.vo.interview;

import com.hanserdev.interview.enums.ReportGenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * WebSocket报告生成通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerationStatusMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private UUID sessionId;

    /**
     * 报告ID（成功时返回）
     */
    private UUID reportId;

    /**
     * 用户ID（用于前端可选校验）
     */
    private Long userId;

    /**
     * 任务状态
     */
    private ReportGenerationStatus status;

    /**
     * 状态描述
     */
    private String message;

    /**
     * 后端发送时间戳（毫秒）
     */
    private long timestamp;
}
