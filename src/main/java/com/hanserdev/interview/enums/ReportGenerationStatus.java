package com.hanserdev.interview.enums;

/**
 * 面试报告生成任务状态
 */
public enum ReportGenerationStatus {
    /**
     * 任务已入队等待执行
     */
    QUEUED,
    /**
     * 正在执行（已拉起AI生成）
     */
    PROCESSING,
    /**
     * 执行成功，报告已入库
     */
    SUCCESS,
    /**
     * 执行失败
     */
    FAILED
}
