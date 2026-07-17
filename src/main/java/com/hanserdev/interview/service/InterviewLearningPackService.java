package com.hanserdev.interview.service;

import com.hanserdev.interview.model.vo.interview.InterviewLearningPackRspVO;

import java.util.UUID;

/**
 * 智能学习包：知识库片段、精选外链、个性化练习计划与推荐面试题。
 */
public interface InterviewLearningPackService {

    InterviewLearningPackRspVO buildLearningPack(UUID sessionId);
}
