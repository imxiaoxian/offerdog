package com.hanserdev.interview.config;

import com.hanserdev.interview.service.impl.CategoryDefaultSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 在示例题库种子之前保证存在二级岗位分类。
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class CategoryDefaultSeedRunner implements CommandLineRunner {

    private final CategoryDefaultSeedService categoryDefaultSeedService;

    @Override
    public void run(String... args) {
        categoryDefaultSeedService.seedIfNoLevel2Categories();
    }
}
