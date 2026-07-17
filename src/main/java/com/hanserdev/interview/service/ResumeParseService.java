package com.hanserdev.interview.service;

import com.hanserdev.interview.model.dto.user.UserResumeDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历解析
 *
 * @Author Zane
 * @CreateTime 2025/11/17 星期一 00:20
 */
public interface ResumeParseService {

    /**
     * 解析简历文件，提取结构化信息
     *
     * @param file 简历文件
     * @return 结构化的简历信息
     */
    UserResumeDTO parseResumeFile(MultipartFile file);

}