package com.xiaobai1202.ddns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DdnsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DdnsApplication.class, args);
    }

}
