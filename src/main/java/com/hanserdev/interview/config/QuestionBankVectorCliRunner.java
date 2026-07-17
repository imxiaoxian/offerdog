package com.hanserdev.interview.config;

import com.hanserdev.interview.service.QuestionVectorService;
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
 * 命令行：将指定题库全部题目写入向量库后退出（需嵌入 API 可用）。
 * <pre>
 * mvn -q spring-boot:run -Dspring-boot.run.profiles=prod,question-vector-cli \
 *   -Dspring-boot.run.arguments="--spring.main.web-application-type=none --question-bank-id=9"
 * </pre>
 * Docker 镜像 ENTRYPOINT 不便传参时可设环境变量 {@code QUESTION_VECTOR_CLI_BANK_ID=9}，并激活 profile {@code question-vector-cli}、{@code SPRING_MAIN_WEB_APPLICATION_TYPE=none}。
 * 全量重建（较慢）：命令行加 {@code --rebuild-all-questions}，或设 {@code QUESTION_VECTOR_CLI_REBUILD_ALL=true}。
 */
@Slf4j
@Component
@Profile("question-vector-cli")
@Order(0)
@RequiredArgsConstructor
public class QuestionBankVectorCliRunner implements ApplicationRunner {

    private final QuestionVectorService questionVectorService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean rebuildAll = args.containsOption("rebuild-all-questions")
                    || "true".equalsIgnoreCase(System.getenv("QUESTION_VECTOR_CLI_REBUILD_ALL"));
            if (rebuildAll) {
                int n = questionVectorService.rebuildVectorStore();
                log.info("题库向量全量重建完成，共索引 {} 道题目", n);
            } else {
                String raw = firstOption(args, "question-bank-id");
                if (raw == null || raw.isBlank()) {
                    raw = System.getenv("QUESTION_VECTOR_CLI_BANK_ID");
                }
                if (raw == null || raw.isBlank()) {
                    log.error("请指定 --question-bank-id=<id> 或环境变量 QUESTION_VECTOR_CLI_BANK_ID，或使用 --rebuild-all-questions / QUESTION_VECTOR_CLI_REBUILD_ALL=true");
                    SpringApplication.exit(applicationContext, () -> 2);
                    System.exit(2);
                    return;
                }
                long bankId = Long.parseLong(raw.trim());
                int n = questionVectorService.addQuestionBank(bankId);
                log.info("题库向量化完成 bankId={}，成功索引 {} 道题目", bankId, n);
            }
        } catch (Exception e) {
            log.error("题库向量化失败", e);
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
