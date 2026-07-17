package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import com.hanserdev.interview.domain.mapper.InterviewExperienceMapper;
import com.hanserdev.interview.service.InterviewExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewExperienceCrawlerService {

    private final InterviewExperienceMapper experienceMapper;
    private final WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build();

    private static final String NOWCODER_BASE_URL = "https://www.nowcoder.com";
    private static final String ZHILIANZHPIN_BASE_URL = "https://www.zhaopin.com";

    @Transactional(rollbackFor = Exception.class)
    public void crawlNowCoder(String companyName, int pageNum) {
        log.info("Crawling NowCoder for company: {}, page: {}", companyName, pageNum);
        
        String searchUrl = String.format("%s/ta/search?query=%s&type=1&page=%d", 
                NOWCODER_BASE_URL, companyName, pageNum);
        
        try {
            String html = fetchHtml(searchUrl);
            List<ExperienceCrawlResult> results = parseNowCoderList(html, companyName);
            
            for (ExperienceCrawlResult result : results) {
                saveExperience(result);
            }
            
            log.info("Crawled {} experiences from NowCoder for {}", results.size(), companyName);
        } catch (Exception e) {
            log.error("Failed to crawl NowCoder: {}", e.getMessage());
        }
    }

    private String fetchHtml(String url) {
        try {
            Mono<String> mono = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class);
            
            return mono.block();
        } catch (Exception e) {
            log.warn("Failed to fetch URL: {}, error: {}", url, e.getMessage());
            return "";
        }
    }

    private List<ExperienceCrawlResult> parseNowCoderList(String html, String companyName) {
        List<ExperienceCrawlResult> results = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("<a[^>]+href=\"(/feed/[^\"]+)\"[^>]*>([^<]+)</a>");
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String detailUrl = NOWCODER_BASE_URL + matcher.group(1);
            String title = matcher.group(2);
            
            ExperienceCrawlResult result = new ExperienceCrawlResult();
            result.setCompanyName(companyName);
            result.setTitle(title);
            result.setSourceUrl(detailUrl);
            result.setSource("NowCoder");
            
            results.add(result);
        }
        
        return results;
    }

    @Transactional(rollbackFor = Exception.class)
    public void crawlAndSaveExperience(ExperienceCrawlResult crawlResult) {
        try {
            String html = fetchHtml(crawlResult.getSourceUrl());
            String content = parseNowCoderDetail(html);
            crawlResult.setContent(content);
            
            saveExperience(crawlResult);
        } catch (Exception e) {
            log.error("Failed to crawl detail: {}", e.getMessage());
        }
    }

    private String parseNowCoderDetail(String html) {
        Pattern pattern = Pattern.compile("<div[^>]+class=\"[^'\"]*feed-detail[^'\"]*\"[^>]*>([\\s\\S]*?)</div>");
        Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return cleanHtml(matcher.group(1));
        }
        return "";
    }

    private void saveExperience(ExperienceCrawlResult crawlResult) {
        LambdaQueryWrapper<InterviewExperienceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewExperienceDO::getSourceUrl, crawlResult.getSourceUrl());
        
        long count = experienceMapper.selectCount(wrapper);
        if (count > 0) {
            log.debug("Experience already exists: {}", crawlResult.getSourceUrl());
            return;
        }
        
        InterviewExperienceDO experience = new InterviewExperienceDO();
        experience.setCompanyName(crawlResult.getCompanyName());
        experience.setPosition(crawlResult.getPosition());
        experience.setExperienceType(crawlResult.getExperienceType());
        experience.setContent(crawlResult.getContent());
        experience.setFormattedContent(formatContent(crawlResult.getContent()));
        experience.setSourceUrl(crawlResult.getSourceUrl());
        experience.setSource(crawlResult.getSource());
        experience.setAuthor(crawlResult.getAuthor());
        experience.setViews(0);
        experience.setLikes(0);
        experience.setCreatedBy(1L);
        experience.setCreatedAt(LocalDateTime.now());
        experience.setUpdatedAt(LocalDateTime.now());
        
        experienceMapper.insert(experience);
        log.info("Saved experience: {} - {}", experience.getCompanyName(), experience.getSourceUrl());
    }

    private String formatContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        content = content.replaceAll("\\\\s+", " ").trim();

        StringBuilder sb = new StringBuilder();
        sb.append("## 面经内容\\n\\n");

        String[] lines = content.split("\\n");
        boolean inQuestion = false;
        boolean inAnswer = false;
        int sectionCount = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("八股") || line.contains("基础题") || containsNumberedList(line)) {
                if (!inQuestion) {
                    sb.append("\\n### 八股文\\n");
                    inQuestion = true;
                    inAnswer = false;
                    sectionCount = 0;
                }
                sectionCount++;
                sb.append(sectionCount).append(". ").append(cleanLine(line)).append("\\n");
            } else if (line.contains("项目") || line.contains("实习") || line.contains("经历")) {
                if (!inAnswer) {
                    sb.append("\\n### 项目/实习\\n");
                    inAnswer = true;
                    inQuestion = false;
                    sectionCount = 0;
                }
                sectionCount++;
                sb.append(sectionCount).append(". ").append(cleanLine(line)).append("\\n");
            } else if (line.contains("手撕") || line.contains("算法") || line.contains("代码")) {
                sb.append("\\n### 手撕\\n");
                sb.append(cleanLine(line)).append("\\n");
            } else {
                if (!inQuestion && !inAnswer) {
                    sb.append(cleanLine(line)).append("\\n");
                }
            }
        }

        return sb.toString();
    }

    private boolean containsNumberedList(String line) {
        return line.matches("^\\\\d+[.、].*") || 
               line.contains("1.") || line.contains("2.") || 
               line.contains("3.") || line.contains("4.") || line.contains("5.");
    }

    private String cleanLine(String line) {
        line = line.replaceAll("^[\\\\d+[.、]]+\\\\s*", "");
        line = line.replaceAll("[^\\\\u4e00-\\\\u9fa5a-zA-Z0-9\\\\s,.!?;:()（）【】《》\"'']", "");
        return line.trim();
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        
        html = html.replaceAll("<script[^>]*>[\\\\s\\\\S]*?</script>", "");
        html = html.replaceAll("<style[^>]*>[\\\\s\\\\S]*?</style>", "");
        html = html.replaceAll("<[^>]+>", "");
        html = html.replaceAll("&nbsp;", " ");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        html = html.replaceAll("&amp;", "&");
        
        return html.trim();
    }

    public static class ExperienceCrawlResult {
        private String companyName;
        private String position;
        private String experienceType = "full_time";
        private String title;
        private String content;
        private String sourceUrl;
        private String source;
        private String author;

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getExperienceType() { return experienceType; }
        public void setExperienceType(String experienceType) { this.experienceType = experienceType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }
}
