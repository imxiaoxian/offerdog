package com.hanserdev.interview.utils;

import com.hanserdev.interview.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class FileUtils {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    private static final long DEFAULT_MAX_FILE_SIZE = 20 * 1024 * 1024;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/",
            "text/markdown",
            "text/x-markdown"
    );

    public String uploadFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        if (fileName == null || fileName.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        try {
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();

            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(putObjectArgs);

            String prefix = minioProperties.getUrlPrefix() == null
                    ? ""
                    : minioProperties.getUrlPrefix().replaceAll("/+$", "");
            String fileUrl = prefix + "/" + minioProperties.getBucketName() + "/" + fileName;

            log.info("文件上传成功，访问URL：{}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败", e);
        } catch (MinioException e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("上传文件到MinIO失败", e);
        } catch (InvalidKeyException e) {
            log.error("MinIO密钥无效", e);
            throw new RuntimeException("MinIO密钥无效", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("MinIO算法不支持", e);
            throw new RuntimeException("MinIO算法不支持", e);
        }
    }

    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            String key = extractFileNameFromUrl(fileUrl);

            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(key)
                    .build();

            minioClient.removeObject(removeObjectArgs);
            log.info("文件删除成功: {}", key);
            return true;

        } catch (MinioException e) {
            log.error("MinIO删除文件失败: {}", e.getMessage(), e);
            return false;
        } catch (InvalidKeyException e) {
            log.error("MinIO密钥无效: {}", e.getMessage(), e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            log.error("MinIO算法不支持: {}", e.getMessage(), e);
            return false;
        } catch (IOException e) {
            log.error("IO异常: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean validateFileSize(MultipartFile file) {
        if (file == null) {
            return false;
        }
        return file.getSize() <= DEFAULT_MAX_FILE_SIZE;
    }

    public boolean validateFileSize(MultipartFile file, long maxSize) {
        if (file == null) {
            return false;
        }
        return file.getSize() <= maxSize;
    }

    public boolean validateFileType(MultipartFile file) {
        if (file == null || file.getContentType() == null) {
            return false;
        }
        return ALLOWED_FILE_TYPES.stream().anyMatch(type ->
                file.getContentType().startsWith(type) ||
                file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(getExtension(type))
        );
    }

    public boolean validateFileContentType(MultipartFile file) {
        return validateFileType(file);
    }

    private String getExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "text/markdown", "text/x-markdown" -> ".md";
            default -> "";
        };
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        int bucketIndex = fileUrl.indexOf(minioProperties.getBucketName());
        if (bucketIndex == -1) {
            return null;
        }
        return fileUrl.substring(bucketIndex + minioProperties.getBucketName().length() + 1);
    }

    public String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public String generateUniqueFilename(MultipartFile file) {
        return generateFileName(file.getOriginalFilename());
    }
}
