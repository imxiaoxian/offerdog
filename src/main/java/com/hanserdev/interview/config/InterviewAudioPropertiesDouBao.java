package com.hanserdev.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置豆包 TTS 参数
 *
 * @Author Zane
 * @CreateTime 2025/11/19 星期三 21:59
 */
@Data
@Component
@ConfigurationProperties(prefix = "doubao")
public class InterviewAudioPropertiesDouBao {

    /**
     * 是否开启语音播报
     */
    private boolean enabled = false;

    /**
     * 豆包 App ID
     */
    private String appId;

    /**
     * 豆包 Access Token
     */
    private String accessToken;

    /**
     * 豆包 Resource ID
     */
    private String resourceId = "";

    /**
     * 语音角色
     */
    private String voice = "ICL_zh_female_zhixingwenwan_tob";

    /**
     * 音频编码格式 (mp3, wav, pcm 等)
     */
    private String encoding = "pcm";

    /**
     * WebSocket 端点
     */
    private String wsEndpoint = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";

    /**
     * 等待豆包结束的超时时间（毫秒）
     */
    private long awaitTimeoutMs = 45000;

    /**
     * 语速，取值范围[-50,100]，100代表2.0倍速，-50代表0.5倍数
     */
    private float speechRate = 10;
}
