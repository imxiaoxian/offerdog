package com.hanserdev.interview.common.advisor;

import com.hanserdev.interview.domain.dataobject.ConversationMessageDO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AiChatMessage2DBAdvisor implements CallAdvisor, StreamAdvisor {

    // 创建事务模板
    private final TransactionTemplate transactionTemplate;
    // 创建会话消息数据对象
    private final ConversationMessageDO conversationMessageDO;

    public AiChatMessage2DBAdvisor(TransactionTemplate transactionTemplate, ConversationMessageDO conversationMessageDO) {
        this.transactionTemplate = transactionTemplate;
        this.conversationMessageDO = conversationMessageDO;
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest chatClientRequest, @NotNull CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        // TODO:保存会话消息
        return null;
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest chatClientRequest, @NotNull StreamAdvisorChain streamAdvisorChain) {
        Flux<ChatClientResponse> chatClientResponseFlux = streamAdvisorChain.nextStream(chatClientRequest);
        // 创建 AI 流式回答聚合容器（线程安全）
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());

        return chatClientResponseFlux
                .doOnNext(response -> {
                    // 逐块收集内容
                    String chunk = null;
                    if (response.chatResponse() != null) {
                        chunk = response.chatResponse().getResult().getOutput().getText();
                    }
                    // 若 chunk 块不为空，则追加到 fullContent 中
                    if (chunk != null) {
                        fullContent.get().append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    // 流完成后打印完整回答
                    String completeResponse = fullContent.get().toString();
                    log.info("\n==== FULL AI STREAM RESPONSE ====\n{}\n=================================", completeResponse);
                    // 开启事务
                    transactionTemplate.execute(transactionStatus -> {
                        // TODO：保存会话消息
                        return true;
                    });
                });
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
