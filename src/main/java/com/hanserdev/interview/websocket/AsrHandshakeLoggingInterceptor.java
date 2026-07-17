package com.hanserdev.interview.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * 记录 WebSocket 握手阶段的关键信息，便于排查前端是否真正连到后端。
 */
@Slf4j
public class AsrHandshakeLoggingInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        log.info("[ASR-WS] beforeHandshake uri={}, remote={}", request.getURI(), remoteAddress);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        log.info("[ASR-WS] afterHandshake uri={}, err={}",
                request.getURI(), exception == null ? "none" : exception.getMessage());
    }
}
