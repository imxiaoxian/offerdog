package com.hanserdev.interview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import com.hanserdev.interview.service.ResumeParseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * ResumeParseService 的实现类
 *
 * @Author Zane
 * @CreateTime 2025/11/17 星期一 00:22
 */
@Slf4j
@Service
public class ResumeParseServiceImpl implements ResumeParseService {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ChatClient dashScopeChatClient;

    private static final String RESUME_PARSE_PROMPT_TEMPLATE = """
            你是一个专业的简历解析助手。请仔细阅读以下简历内容，提取关键信息并按照指定的JSON格式输出。
            
            要求：
            1. 严格按照JSON格式输出，不要添加任何额外的文字说明
            2. 如果某个字段信息不存在，使用null
            3. 日期格式：YYYY-MM-DD（如：1995-08-20）
            4. 年月格式：YYYY-MM（如：2020-07）
            5. 技能列表：提取所有技术栈、工具、语言等
            
            JSON格式示例：
            {
              "name": "张三",
              "birth": "1995-08-20",
              "gender": "男",
              "location": {
                "province": "广东省",
                "city": "深圳市"
              },
              "education": {
                "level": "本科",
                "school": "清华大学",
                "major": "计算机科学与技术",
                "graduationDate": "2020-07"
              },
              "contact": {
                "phone": "+86-138-0013-8000",
                "email": "zhangsan@example.com",
                "homepage": "https://github.com/zhangsan"
              },
              "work": {
                "firstEmployment": "2020-08",
                "experience": [
                  {
                    "company": "字节跳动",
                    "title": "高级Java开发工程师",
                    "startDate": "2022-01",
                    "endDate": "2025-10",
                    "description": "负责推荐系统开发"
                  }
                ],
                "projects": [
                  {
                    "name": "推荐系统升级",
                    "role": "主要开发人员",
                    "description": "实现毫秒级推荐延迟",
                    "highlights": ["性能提升30%", "用户转化率提升5%"]
                  }
                ]
              },
              "skills": ["Java", "Python", "MySQL", "Redis"],
              "jobIntention": "高级Java开发工程师",
              "selfEvaluation": "5年后端开发经验，精通微服务架构"
            }
            
            简历内容：
            {RESUME_CONTENT}
            
            请直接输出JSON，不要包含```json```等标记：
            """;

    @Override
    public UserResumeDTO parseResumeFile(MultipartFile file) {
        try {
            // 1. 提取简历文本内容
            String resumeText = extractTextFromFile(file);

            log.info("提取的简历文本长度: {} 字符", resumeText.length());
            log.info("提取的简历文本前500字符: {}", 
                    resumeText.length() > 500 ? resumeText.substring(0, 500) : resumeText);

            // 2. 使用AI解析简历
            return parseResumeWithAI(resumeText);

        } catch (Exception e) {
            log.error("简历解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("简历解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从MultipartFile提取文本内容
     */
    private String extractTextFromFile(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new RuntimeException("无法识别文件类型");
        }

        try {
            // PDF文件
            if (contentType.equals("application/pdf")) {
                return extractTextFromPdf(file);
            }
            // Word文件或其他文档
            else if (contentType.contains("word") ||
                    contentType.contains("document") ||
                    contentType.equals("text/plain")) {
                return extractTextWithTika(file);
            }
            else {
                throw new RuntimeException("不支持的文件类型: " + contentType);
            }
        } catch (Exception e) {
            log.error("文本提取失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本提取失败");
        }
    }

    /**
     * 从PDF文件提取文本（简化版本）
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        InputStreamResource resource = new InputStreamResource(file.getInputStream());
        
        // 使用默认配置创建 PDF 读取器
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
        List<Document> documents = pdfReader.get();

        // 提取文本内容
        StringBuilder textBuilder = new StringBuilder();
        for (Document doc : documents) {
            try {
                // 尝试获取文档内容
                String content = doc.getText();
                if (content != null && !content.trim().isEmpty()) {
                    textBuilder.append(content).append("\n");
                }
            } catch (Exception e) {
                log.warn("提取PDF页面内容失败: {}", e.getMessage());
            }
        }
        
        String result = textBuilder.toString().trim();
        if (result.isEmpty()) {
            throw new RuntimeException("PDF文件内容为空或无法提取");
        }
        
        return result;
    }

    /**
     * 使用Tika提取文本（支持Word、TXT等）
     */
    private String extractTextWithTika(MultipartFile file) throws IOException {
        InputStreamResource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        List<Document> documents = tikaReader.get();

        StringBuilder textBuilder = new StringBuilder();
        for (Document doc : documents) {
            try {
                String content = doc.getText();
                if (content != null && !content.trim().isEmpty()) {
                    textBuilder.append(content).append("\n");
                }
            } catch (Exception e) {
                log.warn("提取文档内容失败: {}", e.getMessage());
            }
        }
        
        String result = textBuilder.toString().trim();
        if (result.isEmpty()) {
            throw new RuntimeException("文档内容为空或无法提取");
        }
        
        return result;
    }

    /**
     * 使用AI解析简历文本
     */
    private UserResumeDTO parseResumeWithAI(String resumeText) {
        try {
            // 限制文本长度，避免超过API限制
            if (resumeText.length() > 10000) {
                resumeText = resumeText.substring(0, 10000);
                log.warn("简历文本过长，已截断到10000字符");
            }

            // 构建提示词（使用 replace 避免 String.format 的特殊字符问题）
            String prompt = RESUME_PARSE_PROMPT_TEMPLATE.replace("{RESUME_CONTENT}", resumeText);

            String aiResponse = dashScopeChatClient.prompt().user(prompt).call().content();

            log.info("AI解析结果: {}", aiResponse);

            // 清理可能的markdown标记
            aiResponse = aiResponse.trim();
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            }
            if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.substring(3);
            }
            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }
            aiResponse = aiResponse.trim();

            // 解析JSON为UserResumeDTO
            UserResumeDTO resumeDTO = objectMapper.readValue(aiResponse, UserResumeDTO.class);

            log.info("简历解析成功: {}", resumeDTO.getName());
            return resumeDTO;

        } catch (Exception e) {
            log.error("AI解析简历失败，详细错误: {}", e.getMessage(), e);
            // 保留原始异常信息
            throw new RuntimeException("AI解析简历失败: " + e.getMessage(), e);
        }
    }
}
