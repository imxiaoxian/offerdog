package com.hanserdev.interview.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 未启用「豆包 TTS + 语音开关」时使用无操作实现（DeepSeek-only 等场景）。
 * 条件：未开启 tts，或已开启 tts 但未启用豆包（不再走阿里云 Qwen）。
 */
public class NoOpTtsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment e = context.getEnvironment();
        boolean ttsOn = Boolean.parseBoolean(e.getProperty("interview.audio.tts.enabled", "false"));
        boolean doubaoOn = Boolean.parseBoolean(e.getProperty("doubao.enabled", "false"));
        return !ttsOn || !doubaoOn;
    }
}
