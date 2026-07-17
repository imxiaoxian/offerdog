package com.hanserdev.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语音识别配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "interview.audio.asr")
public class InterviewAsrProperties {

    /**
     * 是否启用语音识别转文字。
     */
    private boolean enabled = true;

    /**
     * ASR 提供商: volcengine(火山引擎) 或 funasr(阿里 FunASR) 或 whisper(本地 Whisper)
     */
    private String provider = "whisper";

    /**
     * ASR WebSocket 地址（火山引擎）。
     */
    private String apiUrl = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async";

    /**
     * FunASR WebSocket 地址。
     */
    private String funasrUrl = "ws://localhost:10095";

    /**
     * Whisper 服务地址。
     */
    private String whisperUrl = "http://localhost:5000";

    /**
     * 浏览器麦克风 PCM 的真实采样率多为 48000（与 sample-rate 声明给火山的 16000 可能不一致）。
     * 当前端 init 未带 sampleRate 时，Whisper 链路用此值写 WAV 头；0 表示使用 sample-rate。
     */
    private int whisperDefaultPcmSampleRate = 0;

    /**
     * Whisper 语言：zh、en 等；auto 或空表示自动检测。
     */
    private String whisperLanguage = "auto";

    /**
     * Whisper 伪实时转写：录音过程中按音频片段周期性触发一次转写并推送临时字幕。
     * 注意：会增加 CPU 与 Whisper 调用次数。
     */
    private boolean whisperPseudoRealtimeEnabled = true;

    /**
     * Whisper 伪实时转写：每累计多少毫秒音频触发一次转写。
     * 默认 2000ms，过小会导致频繁调用且结果抖动。
     */
    private int whisperPseudoRealtimeChunkMs = 2000;

    /**
     * Whisper 伪实时转写：每次转写后保留的重叠音频时长（毫秒），用于给下一段提供上下文、降低断句。
     */
    private int whisperPseudoRealtimeOverlapMs = 600;

    /**
     * Whisper 伪实时转写：最小触发间隔（毫秒），用于限流避免在高频音频分片下过度调度。
     */
    private long whisperPseudoRealtimeMinIntervalMs = 1200;

    /**
     * 火山引擎应用 ID。
     */
    private String appId;

    /**
     * 火山引擎访问 token。
     */
    private String token;

    /**
     * 资源 ID。
     */
    private String resourceId = "volc.bigasr.sauc.duration";

    /**
     * 使用的模型名称。
     */
    private String modelName = "bigmodel";

    private int sampleRate = 16000;
    private int bits = 16;
    private int channel = 1;

    private boolean enableItn = true;
    private boolean enablePunc = true;
    private boolean enableDdc = true;
    private boolean showUtterances = true;
    private boolean enableNonstream = true;

    /**
     * 前端一次推送的音频分片建议时长（毫秒），仅用于文档和日志提示。
     */
    private int segmentDurationMs = 200;

    /**
     * WebSocket 读写超时毫秒。
     */
    private long requestTimeoutMs = 60000;

    /**
     * WebSocket ping 间隔秒。
     */
    private long pingIntervalSeconds = 60;
}
