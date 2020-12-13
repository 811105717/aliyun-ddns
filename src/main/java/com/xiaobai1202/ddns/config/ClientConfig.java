package com.xiaobai1202.ddns.config;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ddns.client")
public class ClientConfig {


    private String region;
    private String accessKey;
    private String secretKey;

    @Bean
    public IAcsClient client() throws ClientException {
        System.out.println(region + " " + accessKey + " "+ secretKey);
        IClientProfile profile = DefaultProfile.getProfile(region, accessKey, secretKey);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Domain", "domain.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);
        return client;
    }

}
