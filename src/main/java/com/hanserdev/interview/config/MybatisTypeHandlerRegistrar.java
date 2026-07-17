package com.hanserdev.interview.config;

import com.hanserdev.interview.domain.typehandler.PostgresUUIDTypeHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * 注册自定义的 MyBatis 类型处理器
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MybatisTypeHandlerRegistrar {

    private final SqlSessionFactory sqlSessionFactory;

    @PostConstruct
    public void registerPostgresUuidHandler() {
        sqlSessionFactory.getConfiguration()
                .getTypeHandlerRegistry()
                .register(UUID.class, JdbcType.OTHER, PostgresUUIDTypeHandler.class);
        log.info("Registered PostgresUUIDTypeHandler for UUID <-> JDBC OTHER mapping");
    }
}
