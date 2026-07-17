package com.hanserdev.interview.service;

import com.hanserdev.interview.model.dto.interview.ResumeAnalysisDTO;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;

/**
 * 简历分析服务接口
 */
public interface ResumeAnalysisService {

    /**
     * 分析简历，提取关键信息用于定制化面试
     *
     * @param resume 用户简历
     * @return 简历分析结果
     */
    ResumeAnalysisDTO analyzeResume(UserResumeDTO resume);
}

