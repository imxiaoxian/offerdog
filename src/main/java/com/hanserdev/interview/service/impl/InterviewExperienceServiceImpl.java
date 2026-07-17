package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserdev.interview.domain.dataobject.InterviewExperienceDO;
import com.hanserdev.interview.domain.mapper.InterviewExperienceMapper;
import com.hanserdev.interview.model.dto.user.UserSessionDTO;
import com.hanserdev.interview.model.vo.interview.ExperiencePageReqVO;
import com.hanserdev.interview.model.vo.interview.ExperienceSummaryRspVO;
import com.hanserdev.interview.service.InterviewExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewExperienceServiceImpl implements InterviewExperienceService {

    private final InterviewExperienceMapper experienceMapper;
    private WebClient webClient = WebClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                .compress(true)
                .followRedirect(true)))
            .build();

    private static final String NOWCODER_BASE_URL = "https://www.nowcoder.com";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExperience(InterviewExperienceDO experience, UserSessionDTO currentUser) {
        experience.setCreatedBy(currentUser.getUserId());
        experience.setCreatedAt(LocalDateTime.now());
        experience.setUpdatedAt(LocalDateTime.now());
        experienceMapper.insert(experience);
        return experience.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExperience(Long id, InterviewExperienceDO experience, UserSessionDTO currentUser) {
        experience.setId(id);
        experience.setUpdatedAt(LocalDateTime.now());
        experienceMapper.updateById(experience);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExperience(Long id, UserSessionDTO currentUser) {
        experienceMapper.deleteById(id);
    }

    @Override
    public InterviewExperienceDO getExperience(Long id) {
        return experienceMapper.selectById(id);
    }

    @Override
    public IPage<ExperienceSummaryRspVO> pageExperiences(ExperiencePageReqVO reqVO) {
        Page<InterviewExperienceDO> page = new Page<>(reqVO.getPageNum(), reqVO.getPageSize());
        LambdaQueryWrapper<InterviewExperienceDO> wrapper = new LambdaQueryWrapper<>();
        
        if (reqVO.getCompanyName() != null && !reqVO.getCompanyName().isEmpty()) {
            wrapper.like(InterviewExperienceDO::getCompanyName, reqVO.getCompanyName());
        }
        if (reqVO.getPosition() != null && !reqVO.getPosition().isEmpty()) {
            wrapper.like(InterviewExperienceDO::getPosition, reqVO.getPosition());
        }
        if (reqVO.getExperienceType() != null && !reqVO.getExperienceType().isEmpty()) {
            wrapper.eq(InterviewExperienceDO::getExperienceType, reqVO.getExperienceType());
        }
        
        wrapper.orderByDesc(InterviewExperienceDO::getCreatedAt);
        
        IPage<InterviewExperienceDO> resultPage = experienceMapper.selectPage(page, wrapper);
        
        return resultPage.convert(this::convertToSummaryVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void crawlAndSave(String companyName, String sourceUrl) {
        log.info("Crawling interview experience for company: {}, from: {}", companyName, sourceUrl);
        
        boolean crawled = false;
        
        if (sourceUrl.contains("nowcoder.com") || sourceUrl.contains("juejin") || sourceUrl.contains("zhihu")) {
            crawlNowCoder(companyName, sourceUrl);
            crawled = true;
        } else if (sourceUrl == null || sourceUrl.trim().isEmpty()) {
            crawlNowCoder(companyName, "");
            crawled = true;
        } else {
            crawlGeneric(companyName, sourceUrl);
        }
        
        if (!crawled && companyName != null && !companyName.isEmpty()) {
            saveSampleExperience(companyName);
        }
    }
    
    private void saveSampleExperience(String companyName) {
        String[] sampleContents = getSampleContent(companyName);
        if (sampleContents == null) {
            return;
        }
        
        InterviewExperienceDO experience = new InterviewExperienceDO();
        experience.setCompanyName(companyName);
        experience.setPosition(sampleContents[0]);
        experience.setExperienceType("full_time");
        experience.setContent(sampleContents[1]);
        experience.setFormattedContent(sampleContents[2]);
        experience.setSourceUrl("sample-" + companyName);
        experience.setSource("Sample");
        experience.setAuthor("面经采集");
        experience.setViews(0);
        experience.setLikes(0);
        experience.setCreatedBy(1L);
        experience.setCreatedAt(LocalDateTime.now());
        experience.setUpdatedAt(LocalDateTime.now());
        
        experienceMapper.insert(experience);
        log.info("Saved sample experience for company: {}", companyName);
    }
    
    private String[] getSampleContent(String companyName) {
        if (companyName.contains("阿里") || companyName.contains("阿里巴巴")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.HashMap底层原理 3.concurrentHashMap如何保证线程安全 4.MySQL索引底层原理 5.事务隔离级别 6.项目中遇到的难点如何解决 项目：1.项目介绍 2.为什么要选用这个技术栈 3.如何优化接口性能 4.手撕：反转链表",
                "## 阿里巴巴面经\n\n### 八股文\n1. HashMap底层原理\n2. concurrentHashMap如何保证线程安全\n3. MySQL索引底层原理\n4. 事务隔离级别\n5. 项目中遇到的难点如何解决\n\n### 项目/实习\n1. 项目介绍\n2. 为什么要选用这个技术栈\n3. 如何优化接口性能\n\n### 手撕\n反转链表"
            };
        } else if (companyName.contains("腾讯")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.介绍项目 3.Redis数据类型 4.Redis持久化机制 5.分布式锁原理 6.CAP理论 7.手撕：两数之和 二面：1.项目中遇到的最大挑战 2.如何进行性能优化 3.数据库优化经验 4.手撕：二叉树层序遍历",
                "## 腾讯面经\n\n### 八股文\n1. Redis数据类型\n2. Redis持久化机制\n3. 分布式锁原理\n4. CAP理论\n\n### 项目/实习\n1. 项目中遇到的最大挑战\n2. 如何进行性能优化\n3. 数据库优化经验\n\n### 手撕\n两数之和\n二叉树层序遍历"
            };
        } else if (companyName.contains("字节") || companyName.contains("抖音")) {
            return new String[]{
                "Go后端",
                "一面：1.自我介绍 2.Golang GMP模型 3.Golang GC机制 4.K8s了解多少 5.MySQL事务隔离级别 6.手撕：LRU缓存 二面：1.项目架构设计 2.如何保证高可用 3.熔断限流实现 4.手撕：合并K个有序链表 三面：1.职业规划 2.为什么离职 3.反问环节",
                "## 字节跳动面经\n\n### 八股文\n1. Golang GMP模型\n2. Golang GC机制\n3. K8s了解多少\n4. MySQL事务隔离级别\n\n### 项目/实习\n1. 项目架构设计\n2. 如何保证高可用\n3. 熔断限流实现\n\n### 手撕\nLRU缓存\n合并K个有序链表"
            };
        } else if (companyName.contains("美团")) {
            return new String[]{
                "Java后端",
                "一面：1.自我介绍 2.ArrayList和LinkedList区别 3.HashMap扩容机制 4.Synchronized和ReentrantLock区别 5.MySQL慢查询优化 6.手撕：二分查找 二面：1.项目介绍 2.秒杀系统设计 3.如何防止超卖 4.Redis缓存问题 5.手撕：排序算法",
                "## 美团面经\n\n### 八股文\n1. ArrayList和LinkedList区别\n2. HashMap扩容机制\n3. Synchronized和ReentrantLock区别\n4. MySQL慢查询优化\n\n### 项目/实习\n1. 项目介绍\n2. 秒杀系统设计\n3. 如何防止超卖\n4. Redis缓存问题\n\n### 手撕\n二分查找\n排序算法"
            };
        } else if (companyName.contains("京东")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.Spring Boot启动流程 3.Spring Bean生命周期 4.MyBatis缓存 5.Redis数据结构 6.手撕：链表反转 二面：1.项目经验 2.分布式事务 3.Seata原理 4.如何保证数据一致性",
                "## 京东面经\n\n### 八股文\n1. Spring Boot启动流程\n2. Spring Bean生命周期\n3. MyBatis缓存\n4. Redis数据结构\n\n### 项目/实习\n1. 项目经验\n2. 分布式事务\n3. Seata原理\n4. 如何保证数据一致性\n\n### 手撕\n链表反转"
            };
        } else if (companyName.contains("百度")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.JVM内存模型 3.垃圾回收算法 4.类加载机制 5.手撕：快速排序 二面：1.项目架构 2.微服务拆分 3.服务注册发现 4.配置中心",
                "## 百度面经\n\n### 八股文\n1. JVM内存模型\n2. 垃圾回收算法\n3. 类加载机制\n\n### 项目/实习\n1. 项目架构\n2. 微服务拆分\n3. 服务注册发现\n4. 配置中心\n\n### 手撕\n快速排序"
            };
        } else if (companyName.contains("拼多多")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.HashMap底层 3.ConcurrentHashMap 4.volatile原理 5.JMM模型 6.手撕：判断链表有环 二面：1.项目难点 2.高并发优化 3.缓存雪崩 4.缓存击穿",
                "## 拼多多面经\n\n### 八股文\n1. HashMap底层\n2. ConcurrentHashMap\n3. volatile原理\n4. JMM模型\n\n### 项目/实习\n1. 项目难点\n2. 高并发优化\n3. 缓存雪崩\n4. 缓存击穿\n\n### 手撕\n判断链表有环"
            };
        } else if (companyName.contains("网易")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.Spring源码看过吗 3.AOP原理 4.事务传播行为 5.手撕：堆排序 二面：1.项目介绍 2.消息队列使用 3.如何保证消息不丢失 4.系统设计",
                "## 网易面经\n\n### 八股文\n1. Spring源码看过吗\n2. AOP原理\n3. 事务传播行为\n\n### 项目/实习\n1. 项目介绍\n2. 消息队列使用\n3. 如何保证消息不丢失\n4. 系统设计\n\n### 手撕\n堆排序"
            };
        } else if (companyName.contains("快手")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.MySQL事务 3.索引优化 4.Redis使用场景 5.手撕： TopK 二面：1.项目介绍 2.消息队列选型 3.如何保证高可用 4.手撕：LRU",
                "## 快手面经\n\n### 八股文\n1. MySQL事务\n2. 索引优化\n3. Redis使用场景\n\n### 项目/实习\n1. 项目介绍\n2. 消息队列选型\n3. 如何保证高可用\n\n### 手撕\nTopK\nLRU"
            };
        } else if (companyName.contains("滴滴")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.HashMap 3.ThreadLocal 4.MySQL锁 5.手撕：合并区间 二面：1.项目难点 2.分布式事务 3.性能优化",
                "## 滴滴面经\n\n### 八股文\n1. HashMap\n2. ThreadLocal\n3. MySQL锁\n\n### 项目/实习\n1. 项目难点\n2. 分布式事务\n3. 性能优化\n\n### 手撕\n合并区间"
            };
        } else if (companyName.contains("小红书")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.Redis数据结构 3.MySQL事务隔离 4.分布式锁 5.手撕：环形链表 二面：1.项目架构 2.消息队列 3.如何保证高可用",
                "## 小红书面经\n\n### 八股文\n1. Redis数据结构\n2. MySQL事务隔离\n3. 分布式锁\n\n### 项目/实习\n1. 项目架构\n2. 消息队列\n3. 如何保证高可用\n\n### 手撕\n环形链表"
            };
        } else if (companyName.contains("米哈游")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.Go语言基础 3.Redis使用 4.并发编程 5.手撕：并发_safe 二面：1.游戏服务器架构 2.长连接处理 3.性能优化",
                "## 米哈游面经\n\n### 八股文\n1. Go语言基础\n2. Redis使用\n3. 并发编程\n\n### 项目/实习\n1. 游戏服务器架构\n2. 长连接处理\n3. 性能优化\n\n### 手撕\n并发safe"
            };
        } else if (companyName.contains("携程")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.Spring Boot 3.MySQL优化 4.Redis缓存 5.手撕：二分查找 二面：1.项目经验 2.高并发场景 3.分布式系统",
                "## 携程面经\n\n### 八股文\n1. Spring Boot\n2. MySQL优化\n3. Redis缓存\n\n### 项目/实习\n1. 项目经验\n2. 高并发场景\n3. 分布式系统\n\n### 手撕\n二分查找"
            };
        } else if (companyName.contains("B站") || companyName.contains("bilibili")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.Go并发 3.Redis 4.MySQL 5.手撕：排序算法 二面：1.项目介绍 2.直播架构 3.弹幕系统设计",
                "## B站面经\n\n### 八股文\n1. Go并发\n2. Redis\n3. MySQL\n\n### 项目/实习\n1. 项目介绍\n2. 直播架构\n3. 弹幕系统设计\n\n### 手撕\n排序算法"
            };
        } else if (companyName.contains("蔚来")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.HashMap原理 3.线程池 4.MySQL索引 5.手撕：反转链表 二面：1.项目经验 2.车联网系统 3.IoT数据处理",
                "## 蔚来面经\n\n### 八股文\n1. HashMap原理\n2. 线程池\n3. MySQL索引\n\n### 项目/实习\n1. 项目经验\n2. 车联网系统\n3. IoT数据处理\n\n### 手撕\n反转链表"
            };
        } else if (companyName.contains("理想")) {
            return new String[]{
                "后端开发",
                "一面：1.自我介绍 2.JVM 3.并发编程 4.Redis 5.手撕：二叉树遍历 二面：1.项目架构 2.车机系统 3.实时数据处理",
                "## 理想汽车面经\n\n### 八股文\n1. JVM\n2. 并发编程\n3. Redis\n\n### 项目/实习\n1. 项目架构\n2. 车机系统\n3. 实时数据处理\n\n### 手撕\n二叉树遍历"
            };
        } else if (companyName.contains("小鹏")) {
            return new String[]{
                "Java开发",
                "一面：1.自我介绍 2.Spring Cloud 3.分布式事务 4.MySQL 5.手撕：LRU缓存 二面：1.项目经验 2.自动驾驶系统 3.大数据处理",
                "## 小鹏汽车面经\n\n### 八股文\n1. Spring Cloud\n2. 分布式事务\n3. MySQL\n\n### 项目/实习\n1. 项目经验\n2. 自动驾驶系统\n3. 大数据处理\n\n### 手撕\nLRU缓存"
            };
        }
        
        return null;
     }
 
     private void crawlNowCoder(String companyName, String sourceUrl) {
        try {
            boolean crawled = false;
            
            if (sourceUrl == null || sourceUrl.isEmpty() || sourceUrl.contains("test")) {
                String searchUrl = "https://www.nowcoder.com/ta/search?query=" + java.net.URLEncoder.encode(companyName + "面经", "UTF-8");
                log.info("Trying NowCoder search page: {}", searchUrl);
                
                String html = fetchHtml(searchUrl);
                if (!html.isEmpty() && html.length() > 500) {
                    log.info("Fetched search page, length: {}", html.length());
                    
                    List<String> detailUrls = parseNowCoderList(html);
                    log.info("Found {} URLs from NowCoder search page", detailUrls.size());
                    
                    if (!detailUrls.isEmpty()) {
                        for (String detailUrl : detailUrls) {
                            try {
                                String detailHtml = fetchHtml(detailUrl);
                                if (!detailHtml.isEmpty()) {
                                    crawlNowCoderDetail(companyName, detailUrl, detailHtml);
                                    crawled = true;
                                }
                                Thread.sleep(800);
                            } catch (Exception e) {
                                log.error("Failed to crawl NowCoder detail: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            if (!crawled && (sourceUrl.contains("/feed/") || sourceUrl.contains("/detail/"))) {
                String html = fetchHtml(sourceUrl);
                if (!html.isEmpty()) {
                    crawlNowCoderDetail(companyName, sourceUrl, html);
                    crawled = true;
                }
            }
            
            if (!crawled && !sourceUrl.isEmpty()) {
                String html = fetchHtml(sourceUrl);
                if (!html.isEmpty()) {
                    List<String> detailUrls = parseNowCoderList(html);
                    log.info("Found {} detail URLs from provided page", detailUrls.size());
                    
                    for (String detailUrl : detailUrls) {
                        try {
                            String detailHtml = fetchHtml(detailUrl);
                            crawlNowCoderDetail(companyName, detailUrl, detailHtml);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            log.error("Failed to crawl NowCoder detail: {}", e.getMessage());
                        }
                    }
                    crawled = true;
                }
            }
            
            if (!crawled) {
                log.warn("Could not crawl from NowCoder, falling back to sample data");
            }
        } catch (Exception e) {
            log.error("Failed to crawl NowCoder: {}", e.getMessage());
        }
    }

    private boolean parseNowCoderApiResponse(String companyName, String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode dataNode = rootNode.get("data");
            
            if (dataNode == null || dataNode.isEmpty()) {
                log.info("No data found in NowCoder API response");
                return false;
            }
            
            int count = 0;
            for (JsonNode item : dataNode) {
                String detailUrl = "https://www.nowcoder.com/feed/" + item.get("id").asLong();
                if (isUrlExists(detailUrl)) {
                    continue;
                }
                
                JsonNode contentNode = item.get("content");
                String content = contentNode != null ? contentNode.asText() : "";
                
                if (content == null || content.length() < 20) {
                    continue;
                }
                
                JsonNode authorNode = item.get("author");
                String author = authorNode != null ? authorNode.asText() : "匿名";
                
                InterviewExperienceDO experience = new InterviewExperienceDO();
                experience.setCompanyName(companyName);
                experience.setPosition(extractPosition(content));
                experience.setExperienceType(extractExperienceType(content));
                experience.setContent(content);
                experience.setFormattedContent(formatContent(content));
                experience.setSourceUrl(detailUrl);
                experience.setSource("NowCoder");
                experience.setAuthor(author);
                experience.setViews(item.has("viewCount") ? item.get("viewCount").asInt() : 0);
                experience.setLikes(item.has("likeCount") ? item.get("likeCount").asInt() : 0);
                experience.setCreatedBy(1L);
                experience.setCreatedAt(LocalDateTime.now());
                experience.setUpdatedAt(LocalDateTime.now());
                
                experienceMapper.insert(experience);
                count++;
                log.info("Saved NowCoder experience from API: {} - {}", companyName, detailUrl);
            }
            
            log.info("Crawled {} experiences from NowCoder API for {}", count, companyName);
            return count > 0;
        } catch (Exception e) {
            log.error("Failed to parse NowCoder API response: {}", e.getMessage());
            return false;
        }
    }

    private List<String> parseNowCoderList(String html) {
        List<String> urls = new java.util.ArrayList<>();
        
        Pattern pattern = Pattern.compile("<a[^>]+href=\"(https://www\\.nowcoder\\.com/feed/[^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find() && urls.size() < 10) {
            String url = matcher.group(1);
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        
        if (urls.isEmpty()) {
            pattern = Pattern.compile("href=\"(/feed/[^\"]+)\"");
            matcher = pattern.matcher(html);
            while (matcher.find() && urls.size() < 10) {
                String url = "https://www.nowcoder.com" + matcher.group(1);
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        
        if (urls.isEmpty()) {
            pattern = Pattern.compile("data-url=\"(https://www\\.nowcoder\\.com/feed/[^\"]+)\"");
            matcher = pattern.matcher(html);
            while (matcher.find() && urls.size() < 10) {
                String url = matcher.group(1);
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        
        if (urls.isEmpty()) {
            pattern = Pattern.compile("\"(https://www\\.nowcoder\\.com/feed/\\d+)\"");
            matcher = pattern.matcher(html);
            while (matcher.find() && urls.size() < 10) {
                String url = matcher.group(1);
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        
        if (urls.isEmpty()) {
            pattern = Pattern.compile("/feed/(\\d+)");
            matcher = pattern.matcher(html);
            while (matcher.find() && urls.size() < 10) {
                String url = "https://www.nowcoder.com/feed/" + matcher.group(1);
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }

    private void crawlNowCoderDetail(String companyName, String sourceUrl, String html) {
        if (isUrlExists(sourceUrl)) {
            log.debug("URL already exists: {}", sourceUrl);
            return;
        }
        
        String content = extractNowCoderContent(html);
        if (content.length() < 50) {
            log.warn("Content too short from: {}", sourceUrl);
            return;
        }
        
        String position = extractPosition(content);
        String experienceType = extractExperienceType(content);
        
        InterviewExperienceDO experience = new InterviewExperienceDO();
        experience.setCompanyName(companyName);
        experience.setPosition(position);
        experience.setExperienceType(experienceType);
        experience.setContent(content);
        experience.setFormattedContent(formatContent(content));
        experience.setSourceUrl(sourceUrl);
        experience.setSource("NowCoder");
        experience.setAuthor(extractAuthor(html));
        experience.setViews(0);
        experience.setLikes(0);
        experience.setCreatedBy(1L);
        experience.setCreatedAt(LocalDateTime.now());
        experience.setUpdatedAt(LocalDateTime.now());
        
        experienceMapper.insert(experience);
        log.info("Saved NowCoder experience: {} - {}", companyName, sourceUrl);
    }

    private void crawlGeneric(String companyName, String sourceUrl) {
        try {
            if (isUrlExists(sourceUrl)) {
                log.debug("URL already exists: {}", sourceUrl);
                return;
            }
            
            String html = fetchHtml(sourceUrl);
            if (html.isEmpty()) {
                log.warn("Failed to fetch page: {}", sourceUrl);
                return;
            }
            
            String content = extractGenericContent(html);
            if (content.length() < 50) {
                log.warn("Content too short from: {}", sourceUrl);
                return;
            }
            
            String title = extractTitle(html);
            
            InterviewExperienceDO experience = new InterviewExperienceDO();
            experience.setCompanyName(companyName);
            experience.setPosition(extractPosition(content));
            experience.setExperienceType(extractExperienceType(content));
            experience.setContent(content);
            experience.setFormattedContent(formatContent(content));
            experience.setSourceUrl(sourceUrl);
            experience.setSource(extractSource(sourceUrl));
            experience.setAuthor("匿名");
            experience.setViews(0);
            experience.setLikes(0);
            experience.setCreatedBy(1L);
            experience.setCreatedAt(LocalDateTime.now());
            experience.setUpdatedAt(LocalDateTime.now());
            
            experienceMapper.insert(experience);
            log.info("Saved experience: {} - {}", companyName, sourceUrl);
            
        } catch (Exception e) {
            log.error("Failed to crawl generic: {}", e.getMessage());
        }
    }

    private String fetchHtml(String url) {
        try {
            webClient = webClient.mutate()
                    .defaultCookie("NOWCODER_AUTH", "")
                    .build();
            
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

    private String fetchNowCoderApi(String companyName, int pageNum) {
        String apiUrl = String.format(
            "https://www.nowcoder.com/api/feed/home?type=1&query=%s&page=%d&pageSize=20",
            java.net.URLEncoder.encode(companyName), pageNum);
        
        log.info("Fetching NowCoder API: {}", apiUrl);
        
        try {
            webClient = webClient.mutate()
                    .defaultCookie("NOWCODER_AUTH", "")
                    .build();
            
            Mono<String> mono = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(String.class);
            String response = mono.block();
            log.info("NowCoder API response length: {}", response != null ? response.length() : 0);
            if (response != null && response.length() > 0) {
                log.info("NowCoder API response preview: {}", response.substring(0, Math.min(200, response.length())));
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch NowCoder API: {}, error: {}", apiUrl, e.getMessage());
            return "";
        }
    }

    private boolean isUrlExists(String url) {
        LambdaQueryWrapper<InterviewExperienceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewExperienceDO::getSourceUrl, url);
        return experienceMapper.selectCount(wrapper) > 0;
    }

    private String extractNowCoderContent(String html) {
        Pattern pattern = Pattern.compile("<div[^>]+class=\"[^\"]*feed-detail[^\"]*\"[^>]*>([\\s\\S]*?)</div>");
        Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return cleanHtml(matcher.group(1));
        }
        
        pattern = Pattern.compile("<article[^>]*>([\\s\\S]*?)</article>");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return cleanHtml(matcher.group(1));
        }
        
        return extractGenericContent(html);
    }

    private String extractGenericContent(String html) {
        Pattern pattern = Pattern.compile("<main[^>]*>([\\s\\S]*?)</main>");
        Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return cleanHtml(matcher.group(1));
        }
        
        pattern = Pattern.compile("<div[^>]+class=\"[^\"]*content[^\"]*\"[^>]*>([\\s\\S]*?)</div>");
        matcher = pattern.matcher(html);
        
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(matcher.group(1)).append(" ");
        }
        
        if (sb.length() > 0) {
            return cleanHtml(sb.toString());
        }
        
        pattern = Pattern.compile("<body[^>]*>([\\s\\S]*)</body>");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return cleanHtml(matcher.group(1));
        }
        
        return cleanHtml(html);
    }

    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        pattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return "";
    }

    private String extractAuthor(String html) {
        Pattern pattern = Pattern.compile("<span[^>]+class=\"[^\"]*author[^\"]*\"[^>]*>([^<]+)</span>");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "匿名";
    }

    private String extractSource(String url) {
        if (url.contains("nowcoder.com")) return "NowCoder";
        if (url.contains("zhihu.com")) return "知乎";
        if (url.contains("juejin.cn")) return "掘金";
        if (url.contains("csdn.net")) return "CSDN";
        return "Other";
    }

    private String extractPosition(String content) {
        String[] keywords = {"Java", "Python", "Go", "C++", "前端", "后端", "全栈", "测试", "算法", "客户端", "服务端", "iOS", "Android"};
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return keyword;
            }
        }
        return "后端开发";
    }

    private String extractExperienceType(String content) {
        if (content.contains("实习")) return "internship";
        if (content.contains("春招") || content.contains("秋招") || content.contains("校招")) return "campus";
        return "full_time";
    }

    private String formatContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        content = content.replaceAll("\\s+", " ").trim();

        StringBuilder sb = new StringBuilder();
        sb.append("## 面经内容\n\n");

        String[] sections = content.split("(?:八股|项目|实习|手撕|算法|编码|基础|技术|系统|场景|一面|二面|三面|四面|问：|答：)");

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
        return text.split("(?:\\d+[.、]|\\n)");
    }

    private String cleanLine(String line) {
        line = line.replaceAll("^[\\d+.、\\s]+", "");
        line = line.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s,.!?;:()（）【】《》\"'']", "");
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
        html = html.replaceAll("&quot;", "\"");
        html = html.replaceAll("&#39;", "'");
        html = html.replaceAll("\\s+", " ");
        return html.trim();
    }

    private ExperienceSummaryRspVO convertToSummaryVO(InterviewExperienceDO experience) {
        ExperienceSummaryRspVO vo = new ExperienceSummaryRspVO();
        vo.setId(experience.getId());
        vo.setCompanyName(experience.getCompanyName());
        vo.setPosition(experience.getPosition());
        vo.setExperienceType(experience.getExperienceType());
        vo.setFormattedContent(experience.getFormattedContent());
        vo.setSource(experience.getSource());
        vo.setViews(experience.getViews());
        vo.setLikes(experience.getLikes());
        vo.setCreatedAt(experience.getCreatedAt());
        return vo;
    }
}
