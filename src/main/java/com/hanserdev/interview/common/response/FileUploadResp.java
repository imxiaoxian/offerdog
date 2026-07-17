package com.hanserdev.interview.common.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传相应
 *
 * @Author Zane
 * @CreateTime 2025/11/16 星期日 23:51
 */
@Data
@Builder
public class FileUploadResp {

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;
}
