package com.hanserdev.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String bucketName;
    private String endpoint;
    private String secretId;
    private String secretKey;
    private String appId;
    private String region;
}