package com.xiaobai1202.ddns.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ddns.client.config")
public class ConfigProperties {

    private String domain;

    private String rr;

    private String recordType;
}
