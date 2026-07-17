package com.hanserdev.interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class VirtualThreadPoolConfig implements AsyncConfigurer {

    /**
     * 定义一个使用虚拟线程的 Executor Bean。
     * 它会为提交给它的每个任务创建一个新的虚拟线程。
     */
    @Bean(name = "TaskExecutor")
    public Executor TaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}