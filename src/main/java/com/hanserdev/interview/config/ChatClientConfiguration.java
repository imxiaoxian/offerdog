package com.hanserdev.interview.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 对话与结构化抽取使用的 LLM：DeepSeek（OpenAI 兼容 HTTP API）。
 */
@Configuration
public class ChatClientConfiguration {

    @Bean
    @Qualifier("deepSeekOpenAiApi")
    public OpenAiApi deepSeekOpenAiApi(
            @Value("${interview.llm.deepseek.api-key}") String apiKey,
            @Value("${interview.llm.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${interview.llm.deepseek.connect-timeout-ms:30000}") int connectTimeoutMs,
            @Value("${interview.llm.deepseek.read-timeout-ms:600000}") int readTimeoutMs) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "缺少 DeepSeek API Key：请在环境变量或 .env 中设置 DEEPSEEK_API_KEY（对应 interview.llm.deepseek.api-key）");
        }
        String normalizedBase = baseUrl.replaceAll("/$", "");

        java.net.http.HttpClient jdkHttp = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttp);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

        reactor.netty.http.client.HttpClient nettyHttp = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeoutMs));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(nettyHttp));

        return OpenAiApi.builder()
                .baseUrl(normalizedBase)
                .apiKey(apiKey.trim())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }

    @Bean
    @Qualifier("deepSeekChatModel")
    public OpenAiChatModel deepSeekChatModel(
            @Qualifier("deepSeekOpenAiApi") OpenAiApi deepSeekOpenAiApi,
            @Value("${interview.llm.deepseek.chat-model:deepseek-chat}") String model) {
        return OpenAiChatModel.builder()
                .openAiApi(deepSeekOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.3)
                        .build())
                .build();
    }

    @Bean("dashScopeChatClient")
    public ChatClient dashScopeChatClient(@Qualifier("deepSeekChatModel") OpenAiChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel).build();
    }
}
