package com.hanserdev.interview.controller;

import com.hanserdev.interview.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 头像经后端从 MinIO 流出，避免外网/隧道场景下浏览器无法访问 MINIO_URL_PREFIX 直链。
 */
@RestController
@RequestMapping("/public/avatars")
public class PublicAvatarController {

    private static final Pattern AVATAR_KEY = Pattern.compile(
            "^avatar\\d+\\.(png|jpe?g|gif|webp|bmp|heic|heif)$",
            Pattern.CASE_INSENSITIVE);

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    @GetMapping("/{key}")
    public ResponseEntity<StreamingResponseBody> getAvatar(@PathVariable("key") String key) {
        if (key == null || key.contains("/") || key.contains("..") || !AVATAR_KEY.matcher(key).matches()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = guessMediaType(key);
        StreamingResponseBody body = outputStream -> {
            try (GetObjectResponse in = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(key)
                            .build())) {
                in.transferTo(outputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(body);
    }

    private static MediaType guessMediaType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
