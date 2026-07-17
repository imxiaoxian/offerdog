package com.hanserdev.interview.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import com.hanserdev.interview.domain.mapper.InterviewExperienceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewExperienceCrawlerJob {

    private final InterviewExperienceMapper experienceMapper;

    private static final int THREAD_POOL_SIZE = 3;
    private static final int TIMEOUT_SECONDS = 15;
    private static final int MAX_RETRIES = 2;

    private static final List<String> TARGET_COMPANIES = List.of(
            "阿里巴巴", "腾讯", "字节跳动", "美团", "京东",
            "百度", "拼多多", "网易", "快手", "滴滴",
            "华为", "小米", "B站", "小红书", "携程"
    );

    private static final Map<String, String> SOURCE_CONFIGS = Map.of(
            "nowcoder", "https://www.nowcoder.com/ta/search?query=%s&type=1",
            "zhilian", "https://www.zhaopin.com/sou/?jl=%s&el=12800&p=1"
    );

    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Transactional(rollbackFor = Exception.class)
    public void runCrawlerJob() {
        log.info("========== Starting interview experience crawler job ==========");
        
        for (String company : TARGET_COMPANIES) {
            executor.submit(() -> crawlCompany(company));
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("========== Crawler job completed ==========");
    }

    private void crawlCompany(String company) {
        log.info("Crawling company: {}", company);
        
        for (Map.Entry<String, String> entry : SOURCE_CONFIGS.entrySet()) {
            String source = entry.getKey();
            String baseUrl = entry.getValue();
            
            try {
                String url = String.format(baseUrl, company);
                List<String> urls = crawlListPage(source, url, company);
                
                for (String detailUrl : urls) {
                    crawlAndSaveDetail(source, detailUrl, company);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("Failed to crawl {} from {}: {}", company, source, e.getMessage());
            }
        }
    }

    private List<String> crawlListPage(String source, String url, String company) {
        List<String> urls = new ArrayList<>();
        
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_SECONDS * 1000)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("Failed to fetch list page: {}, status: {}", url, response.getStatus());
                return urls;
            }
            
            String html = response.body();
            
            Pattern pattern = Pattern.compile("href=[\"']([^\"']+)[\"'][^>]*>([^<]+)</a>");
            Matcher matcher = pattern.matcher(html);
            
            while (matcher.find() && urls.size() < 10) {
                String href = matcher.group(1);
                if (href.contains("nowcoder") || href.contains("zhaopin") || href.contains("job")) {
                    if (!href.startsWith("http")) {
                        href = "https://www.nowcoder.com" + href;
                    }
                    if (!urls.contains(href)) {
                        urls.add(href);
                    }
                }
            }
            
            log.info("Found {} URLs for {} from {}", urls.size(), company, source);
        } catch (Exception e) {
            log.error("Failed to crawl list page {}: {}", url, e.getMessage());
        }
        
        return urls;
    }

    @Transactional(rollbackFor = Exception.class)
    private void crawlAndSaveDetail(String source, String url, String company) {
        if (isUrlExists(url)) {
            log.debug("URL already exists: {}", url);
            return;
        }
        
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_SECONDS * 1000)
                    .execute();
            
            if (!response.isOk()) {
                log.warn("Failed to fetch detail page: {}, status: {}", url, response.getStatus());
                return;
            }
            
            String html = response.body();
            
            String title = extractTitle(html);
            String content = extractContent(html);
            String author = extractAuthor(html);
            
            if (content.length() < 100) {
                log.debug("Content too short for: {}", url);
                return;
            }
            
            InterviewExperienceDO experience = new InterviewExperienceDO();
            experience.setCompanyName(company);
            experience.setPosition(extractPosition(title, content));
            experience.setExperienceType(extractExperienceType(content));
            experience.setContent(content);
            experience.setFormattedContent(formatContent(content));
            experience.setSourceUrl(url);
            experience.setSource(source);
            experience.setAuthor(author);
            experience.setViews(0);
            experience.setLikes(0);
            experience.setCreatedBy(1L);
            experience.setCreatedAt(LocalDateTime.now());
            experience.setUpdatedAt(LocalDateTime.now());
            
            experienceMapper.insert(experience);
            log.info("Saved experience: {} - {}", company, url);
            
        } catch (Exception e) {
            log.error("Failed to crawl detail {}: {}", url, e.getMessage());
        }
    }

    private boolean isUrlExists(String url) {
        LambdaQueryWrapper<InterviewExperienceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewExperienceDO::getSourceUrl, url);
        return experienceMapper.selectCount(wrapper) > 0;
    }

    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        pattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return "";
    }

    private String extractContent(String html) {
        Pattern pattern = Pattern.compile(
            "(?:<div[^>]*class=[\"'][^\"']*(?:content|detail|article|post)[^\"']*[\"'][^>]*>|<main[^>]*>)([\\s\\S]*?)</(?:div|main|article|section)>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        
        String text;
        if (matcher.find()) {
            text = matcher.group(1);
        } else {
            pattern = Pattern.compile("<body[^>]*>([\\s\\S]*)</body>", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                text = matcher.group(1);
            } else {
                text = html;
            }
        }
        
        return cleanText(text);
    }

    private String extractAuthor(String html) {
        Pattern pattern = Pattern.compile(
            "(?:<span[^>]*class=[\"'][^\"']*(?:author|username|nickname)[^\"']*[\"'][^>]*>|<a[^>]*class=[\"'][^\"']*user[^\"']*[\"'][^>]*>)([^<]+)</(?:span|a)>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "匿名";
    }

    private String extractPosition(String title, String content) {
        Pattern pattern = Pattern.compile("(Java|Python|Go|C\\+\\+|前端|后端|全栈|测试|算法|客户端|服务端|服务端开发|iOS|Android|客户端开发)");
        Matcher matcher = pattern.matcher(title + " " + content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "服务端开发";
    }

    private String extractExperienceType(String content) {
        if (content.contains("实习")) {
            return "internship";
        } else if (content.contains("春招") || content.contains("秋招") || content.contains("校招")) {
            return "campus";
        }
        return "full_time";
    }

    private String formatContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        content = content.replaceAll("\\s+", " ").trim();

        StringBuilder sb = new StringBuilder();
        sb.append("## 面经内容\n\n");

        String[] sections = content.split("(?:八股|项目|实习|手撕|算法|编码|基础|技术|系统|场景)");
        
        boolean hasQuestions = false;
        boolean hasProjects = false;
        boolean hasCoding = false;
        
        int qCount = 0, pCount = 0;
        
        for (String section : sections) {
            section = section.trim();
            if (section.length() < 10) continue;
            
            if (!hasQuestions && isQuestionSection(section)) {
                sb.append("\n### 八股文\n");
                String[] items = splitToItems(section);
                for (String item : items) {
                    if (item.length() > 5) {
                        qCount++;
                        sb.append(qCount).append(". ").append(cleanLine(item)).append("\n");
                    }
                }
                hasQuestions = true;
            } else if (!hasProjects && isProjectSection(section)) {
                sb.append("\n### 项目/实习\n");
                String[] items = splitToItems(section);
                for (String item : items) {
                    if (item.length() > 5) {
                        pCount++;
                        sb.append(pCount).append(". ").append(cleanLine(item)).append("\n");
                    }
                }
                hasProjects = true;
            } else if (!hasCoding && isCodingSection(section)) {
                sb.append("\n### 手撕\n");
                sb.append(cleanLine(section)).append("\n");
                hasCoding = true;
            }
        }

        return sb.toString();
    }

    private boolean isQuestionSection(String text) {
        return text.matches(".*[1-9].*") && 
               (text.contains("问") || text.contains("题") || text.contains("问题") ||
                text.contains("是什么") || text.contains("如何") || text.contains("为什么"));
    }

    private boolean isProjectSection(String text) {
        return text.contains("项目") || text.contains("经历") || 
               text.contains("负责") || text.contains("开发") || text.contains("优化");
    }

    private boolean isCodingSection(String text) {
        return text.contains("手撕") || text.contains("算法") || 
               text.contains("代码") || text.contains("实现") || text.contains("写");
    }

    private String[] splitToItems(String text) {
        return text.split("(?:\\d+[.、]|\\n|(?:^|\\s)(?=[一二三四五六七八九十]))");
    }

    private String cleanLine(String line) {
        line = line.replaceAll("^[\\d+.、\\s]+", "");
        line = line.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s,.!?;:()（）【】《》\"'']", "");
        return line.trim();
    }

    private String cleanText(String text) {
        if (text == null) return "";
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("&[a-zA-Z]+;|&#\\d+;", " ");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }
}
