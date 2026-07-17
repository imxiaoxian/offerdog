package com.hanserdev.interview.service;

import com.hanserdev.interview.model.vo.interview.InterviewGrowthPointRspVO;
import com.hanserdev.interview.model.vo.interview.ReportDetailRspVO;

import java.util.List;
import java.util.UUID;

/**
 * 面试报告服务接口
 */
public interface InterviewReportService {

    /**
     * 生成面试报告（使用虚拟线程池异步执行）
     *
     * @param sessionId 会话ID
     */
    void generateReport(UUID sessionId);

    /**
     * 获取报告详情
     *
     * @param sessionId 会话ID
     * @return 报告详情
     */
    ReportDetailRspVO getReportBySessionId(UUID sessionId);

    /**
     * 列出用户已生成报告的会话得分序列（按报告时间升序，便于画成长曲线）。
     *
     * @param userId 用户 ID
     * @param limit  最多返回条数（含无报告会话时已过滤）
     */
    List<InterviewGrowthPointRspVO> listGrowthPoints(Long userId, int limit);
}

