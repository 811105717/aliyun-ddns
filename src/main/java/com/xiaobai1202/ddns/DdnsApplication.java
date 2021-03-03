package com.xiaobai1202.ddns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.xiaobai1202.ddns.*",
        "com.xiaobai1202.email.generated.*"
})
public class DdnsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdnsApplication.class, args);
    }

}
