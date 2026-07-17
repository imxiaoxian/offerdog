package com.hanserdev.interview.config;

import com.hanserdev.interview.service.impl.SampleQuestionDataSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 可选：启动时写入示例题库并写入向量库（需嵌入模型可用）。
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "interview.seed-sample-questions", havingValue = "true", matchIfMissing = true)
public class SampleQuestionDataSeedRunner implements CommandLineRunner {

    private final SampleQuestionDataSeedService sampleQuestionDataSeedService;

    @Override
    public void run(String... args) {
        log.info("interview.seed-sample-questions=true，开始写入示例题库…");
        sampleQuestionDataSeedService.seedIfNeeded();
    }
}
