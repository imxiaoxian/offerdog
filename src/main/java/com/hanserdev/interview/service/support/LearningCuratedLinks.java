package com.hanserdev.interview.service.support;

import com.hanserdev.interview.model.vo.interview.ExternalLearningLinkRspVO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 按薄弱点关键词匹配的公开学习入口（精选外链），补充向量库未覆盖的资源。
 */
public final class LearningCuratedLinks {

    private LearningCuratedLinks() {
    }

    public static List<ExternalLearningLinkRspVO> pickForWeakPoints(List<String> weakPoints) {
        if (weakPoints == null || weakPoints.isEmpty()) {
            return defaultGeneral();
        }
        String blob = String.join(" ", weakPoints).toLowerCase(Locale.ROOT);
        Set<String> seenUrls = new LinkedHashSet<>();
        List<ExternalLearningLinkRspVO> out = new ArrayList<>();

        addIf(blob, seenUrls, out, "微服务", link(
                "Spring 官方指南",
                "https://spring.io/guides",
                "微服务、Spring Boot、数据访问等官方入门与进阶指南。",
                "微服务"));
        addIf(blob, seenUrls, out, "spring", link(
                "Spring 官方指南",
                "https://spring.io/guides",
                "微服务、Spring Boot、数据访问等官方入门与进阶指南。",
                "Spring"));
        addIf(blob, seenUrls, out, "java", link(
                "Java 语言与 JVM 参考",
                "https://docs.oracle.com/en/java/",
                "Oracle Java 文档，适合查语言与标准库细节。",
                "Java"));
        addIf(blob, seenUrls, out, "缓存", link(
                "Redis 官方文档",
                "https://redis.io/docs/",
                "数据结构、持久化、集群与最佳实践。",
                "缓存"));
        addIf(blob, seenUrls, out, "redis", link(
                "Redis 官方文档",
                "https://redis.io/docs/",
                "数据结构、持久化、集群与最佳实践。",
                "Redis"));
        addIf(blob, seenUrls, out, "mysql", link(
                "MySQL 8.0 参考手册",
                "https://dev.mysql.com/doc/refman/8.0/en/",
                "SQL、索引、事务与优化器说明。",
                "MySQL"));
        addIf(blob, seenUrls, out, "消息队列", link(
                "Apache Kafka 文档",
                "https://kafka.apache.org/documentation/",
                "生产者、消费者、分区与可靠性语义。",
                "消息队列"));
        addIf(blob, seenUrls, out, "kafka", link(
                "Apache Kafka 文档",
                "https://kafka.apache.org/documentation/",
                "生产者、消费者、分区与可靠性语义。",
                "Kafka"));
        addIf(blob, seenUrls, out, "kubernetes", link(
                "Kubernetes 中文文档",
                "https://kubernetes.io/zh-cn/docs/home/",
                "工作负载、Service、Deployment 与排障。",
                "Kubernetes"));
        addIf(blob, seenUrls, out, "docker", link(
                "Docker 文档",
                "https://docs.docker.com/",
                "镜像、容器、Compose 与构建优化。",
                "Docker"));
        addIf(blob, seenUrls, out, "vue", link(
                "Vue 3 官方文档",
                "https://cn.vuejs.org/guide/introduction.html",
                "组合式 API、响应式与工程化建议。",
                "Vue"));
        addIf(blob, seenUrls, out, "react", link(
                "React 官方文档",
                "https://react.dev/learn",
                "组件、Hooks 与性能相关概念。",
                "React"));
        addIf(blob, seenUrls, out, "javascript", link(
                "MDN JavaScript",
                "https://developer.mozilla.org/zh-CN/docs/Web/JavaScript",
                "语言基础、异步与运行时语义。",
                "JavaScript"));
        addIf(blob, seenUrls, out, "typescript", link(
                "TypeScript 手册",
                "https://www.typescriptlang.org/docs/",
                "类型系统、泛型与工程配置。",
                "TypeScript"));
        addIf(blob, seenUrls, out, "算法", link(
                "LeetCode 题库",
                "https://leetcode.cn/problemset/",
                "按标签刷题，巩固算法与数据结构。",
                "算法"));
        addIf(blob, seenUrls, out, "动态规划", link(
                "OI Wiki 动态规划",
                "https://oi-wiki.org/dp/",
                "DP 模型与常见题型梳理。",
                "动态规划"));
        addIf(blob, seenUrls, out, "网络", link(
                "HTTP/网络 MDN",
                "https://developer.mozilla.org/zh-CN/docs/Web/HTTP",
                "HTTP、缓存、CORS 与状态码。",
                "计算机网络"));

        if (out.isEmpty()) {
            out.addAll(defaultGeneral());
        }
        return out;
    }

    private static void addIf(String blob, Set<String> seenUrls, List<ExternalLearningLinkRspVO> out,
            String keyword, ExternalLearningLinkRspVO link) {
        if (!blob.contains(keyword)) {
            return;
        }
        if (seenUrls.add(link.getUrl())) {
            out.add(link);
        }
    }

    private static ExternalLearningLinkRspVO link(String title, String url, String description, String topicTag) {
        ExternalLearningLinkRspVO vo = new ExternalLearningLinkRspVO();
        vo.setTitle(title);
        vo.setUrl(url);
        vo.setDescription(description);
        vo.setTopicTag(topicTag);
        return vo;
    }

    private static List<ExternalLearningLinkRspVO> defaultGeneral() {
        List<ExternalLearningLinkRspVO> list = new ArrayList<>();
        list.add(link(
                "MDN Web 文档",
                "https://developer.mozilla.org/zh-CN/",
                "前端与 Web 平台通用参考。",
                "通用"));
        list.add(link(
                "计算机教育开源计划 CS-Notes",
                "https://github.com/CyC2018/CS-Notes",
                "技术面试综合笔记（分布式、数据库、网络等）。",
                "综合"));
        return list;
    }
}
