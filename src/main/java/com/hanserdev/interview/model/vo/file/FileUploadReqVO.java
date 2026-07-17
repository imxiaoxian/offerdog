package com.hanserdev.interview.model.vo.file;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求VO
 *
 * @Author Zane
 * @CreateTime 2025/11/16 星期日 23:46
 */
@Data
@Builder
public class FileUploadReqVO {

    /**
     * 上传的文件
     */
    private MultipartFile file;

    /**
     * 文件类型
     * 如：resume、avatar、document
     */
    private String fileType;
}
