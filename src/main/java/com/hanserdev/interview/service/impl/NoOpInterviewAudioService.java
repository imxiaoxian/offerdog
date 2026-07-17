package com.hanserdev.interview.service.impl;

import com.hanserdev.interview.config.NoOpTtsCondition;
import com.hanserdev.interview.service.InterviewAudioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 不进行语音合成（不使用阿里云 DashScope / 未配置豆包 TTS 时）。
 */
@Slf4j
@Service
@Conditional(NoOpTtsCondition.class)
public class NoOpInterviewAudioService implements InterviewAudioService {

    @Override
    public void streamInterviewerSpeech(UUID sessionId, int sequenceNumber, String text) {
        log.trace("TTS 已关闭（NoOp），跳过播报 sessionId={} seq={}", sessionId, sequenceNumber);
    }
}
