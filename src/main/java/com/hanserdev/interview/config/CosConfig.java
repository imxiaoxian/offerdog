package com.hanserdev.interview.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosConfig {

    @Resource
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient() {
        // 1. 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
        // 2. 设置bucket的地域
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        // 可选: 设置使用https(5.6.54版本之后默认是https)
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3. 生成cosClient客户端
        return new COSClient(cred, clientConfig);
    }
}