package com.hanserdev.interview.config;

import com.hanserdev.interview.service.InterviewKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 可选：启动时从 classpath 导入知识库（需 SiliconFlow 等嵌入可用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "interview.knowledge-base.import-on-startup", havingValue = "true")
public class KnowledgeBaseStartupImporter implements CommandLineRunner {

    private final InterviewKnowledgeBaseService interviewKnowledgeBaseService;

    @Override
    public void run(String... args) {
        log.info("interview.knowledge-base.import-on-startup=true，开始导入知识库...");
        int n = interviewKnowledgeBaseService.importFromClasspath(true);
        log.info("知识库启动导入完成，共 {} 块", n);
    }
}
