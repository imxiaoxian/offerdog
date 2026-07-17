package com.hanserdev.interview.service.impl;

import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.service.ResumeUploadService;
import com.hanserdev.interview.utils.FileUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * ResumeUploadService的实现类
 *
 * @Author Zane
 * @CreateTime 2025/11/16 星期日 22:44
 */
@Service
@Slf4j
public class ResumeUploadServiceImpl implements ResumeUploadService {

    @Resource
    private FileUtils fileUtils;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Override
    public String uploadResume(MultipartFile file) {
        if (!fileUtils.validateFileContentType(file)) {
            log.warn("简历类型校验未通过: contentType={}", file != null ? file.getContentType() : null);
            throw new ApiException(ResponseCodeEnum.RESUME_FILE_INVALID);
        }
        if (!fileUtils.validateFileSize(file, MAX_FILE_SIZE)) {
            log.warn("简历超过大小限制: size={}", file != null ? file.getSize() : 0);
            throw new ApiException(ResponseCodeEnum.RESUME_FILE_INVALID);
        }
        String uniqueFileName = fileUtils.generateUniqueFilename(file);
        String fileUrl = fileUtils.uploadFile(file, uniqueFileName);
        log.info("简历上传成功，访问URL：{}", fileUrl);
        return fileUrl;
    }

}
