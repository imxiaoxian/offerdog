package com.hanserdev.interview.service;

import java.util.UUID;

/**
 * 面试官语音播报服务。
 */
public interface InterviewAudioService {

    /**
     * 将 AI 回复转为语音并通过 WebSocket 推流。
     *
     * @param sessionId      会话 ID
     * @param sequenceNumber 本次 AI 回复的消息序号
     * @param text           AI 的文本回复
     */
    void streamInterviewerSpeech(UUID sessionId, int sequenceNumber, String text);
}
