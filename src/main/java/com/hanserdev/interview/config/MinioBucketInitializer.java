package com.hanserdev.interview.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保简历/头像等使用的桶存在，避免首次上传返回 500。
 * <p>
 * 头像对象名为 {@code avatar{userId}.*}，对 {@code avatar*} 开放匿名读，便于浏览器直接加载 URL；
 * 简历等随机 UUID 对象名仍保持私有。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void run(ApplicationArguments args) {
        String bucket = minioProperties.getBucketName();
        if (StringUtils.isBlank(bucket)) {
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
            applyAvatarPublicReadPolicy(bucket);
        } catch (Exception e) {
            log.warn("MinIO bucket check/create skipped (is MinIO running?): {}", e.getMessage());
        }
    }

    private void applyAvatarPublicReadPolicy(String bucket) {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/avatar*"]
                    }
                  ]
                }
                """.formatted(bucket);
        try {
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            log.debug("MinIO bucket policy set: public GetObject for {}/avatar*", bucket);
        } catch (Exception e) {
            log.warn("MinIO set bucket policy failed (avatars may not load in browser): {}", e.getMessage());
        }
    }
}
