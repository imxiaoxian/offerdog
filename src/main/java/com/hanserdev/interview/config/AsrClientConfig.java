package com.hanserdev.interview.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsrClientConfig {

    @Bean("asrOkHttpClient")
    public OkHttpClient asrOkHttpClient(InterviewAsrProperties properties) {
        return new OkHttpClient.Builder()
                .pingInterval(properties.getPingIntervalSeconds(), TimeUnit.SECONDS)
                .readTimeout(properties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                .callTimeout(properties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean("whisperWebClient")
    public WebClient whisperWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }
}
