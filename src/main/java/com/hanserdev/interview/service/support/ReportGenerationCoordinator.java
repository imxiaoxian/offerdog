package com.hanserdev.interview.service.support;

import com.hanserdev.interview.enums.ReportGenerationStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 负责协调同一场面试的报告生成任务，避免重复提交
 */
@Component
public class ReportGenerationCoordinator {

    private final ConcurrentMap<UUID, ReportGenerationStatus> inFlightTasks = new ConcurrentHashMap<>();

    /**
     * 尝试将任务标记为已排队，如果已有任务存在则返回 false
     */
    public boolean tryMarkQueued(UUID sessionId) {
        return inFlightTasks.putIfAbsent(sessionId, ReportGenerationStatus.QUEUED) == null;
    }

    /**
     * 查询当前任务状态
     */
    public Optional<ReportGenerationStatus> getStatus(UUID sessionId) {
        return Optional.ofNullable(inFlightTasks.get(sessionId));
    }

    /**
     * 将任务状态更新为执行中
     */
    public void markProcessing(UUID sessionId) {
        inFlightTasks.put(sessionId, ReportGenerationStatus.PROCESSING);
    }

    /**
     * 任务完成或失败后清理状态
     */
    public void clear(UUID sessionId) {
        inFlightTasks.remove(sessionId);
    }
}
