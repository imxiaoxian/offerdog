package com.hanserdev.interview;

import org.aspectj.lang.annotation.Aspect;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@Aspect
@MapperScan("com.hanserdev.interview.domain.mapper")
public class InAiInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InAiInterviewApplication.class, args);
    }

}
