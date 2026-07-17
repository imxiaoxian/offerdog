package com.hanserdev.interview.config;

import com.hanserdev.interview.websocket.InterviewAsrWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class InterviewAsrWebSocketConfig implements WebSocketConfigurer {

    private final InterviewAsrWebSocketHandler interviewAsrWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewAsrWebSocketHandler, "/ws/asr")
                .addInterceptors(new com.hanserdev.interview.websocket.AsrHandshakeLoggingInterceptor())
                .setAllowedOriginPatterns("*");
    }
}
