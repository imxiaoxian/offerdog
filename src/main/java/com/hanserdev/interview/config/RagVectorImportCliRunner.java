package com.hanserdev.interview.config;

import com.hanserdev.interview.service.InterviewKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 一次性将本地 RAG 目录文件向量化入库后退出进程。
 * 用法（在项目根目录、依赖 Postgres/Redis/嵌入 API 已就绪）：
 * <pre>
 * mvn -q spring-boot:run -Dspring-boot.run.profiles=prod,rag-import-cli \
 *   -Dspring-boot.run.arguments=--spring.main.web-application-type=none
 * </pre>
 * 可选参数：{@code --rag-dir=C:\\path\\to\\RAG}、{@code --kb-category=后端}、{@code --no-replace}。
 * 分块策略见 {@code interview.knowledge-base.chunk-strategy}（recursive-semantic：递归 + 语义合并 + 重叠）。
 */
@Slf4j
@Component
@Profile("rag-import-cli")
@Order(0)
@RequiredArgsConstructor
public class RagVectorImportCliRunner implements ApplicationRunner {

    private final InterviewKnowledgeBaseService interviewKnowledgeBaseService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        String dir = firstOption(args, "rag-dir");
        String cat = firstOption(args, "kb-category");
        if (cat == null || cat.isBlank()) {
            cat = "后端";
        }
        boolean replace = !args.containsOption("no-replace");
        try {
            int n = interviewKnowledgeBaseService.importFromRagDirectory(dir, cat, replace);
            log.info("RAG 向量化入库完成，共写入 {} 个文本块", n);
        } catch (Exception e) {
            log.error("RAG 向量化失败", e);
            SpringApplication.exit(applicationContext, () -> 1);
            System.exit(1);
            return;
        }
        int code = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(code);
    }

    private static String firstOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            return null;
        }
        var values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
