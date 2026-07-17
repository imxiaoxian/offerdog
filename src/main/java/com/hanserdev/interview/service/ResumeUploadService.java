package com.hanserdev.interview.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 简历上传
 *
 * @Author Zane
 * @CreateTime 2025/11/16 星期日 22:42
 */
public interface ResumeUploadService {

    /**
     * 上传简历
     *
     * @param file 简历文件
     * @return COS访问地址
     */
    String uploadResume(MultipartFile file);
}
