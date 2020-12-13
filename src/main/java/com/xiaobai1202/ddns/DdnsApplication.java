package com.xiaobai1202.ddns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;

@EnableScheduling
@SpringBootApplication
public class DdnsApplication {

    public static void main(String[] args) throws IOException {
        makeChangeLog();
        SpringApplication.run(DdnsApplication.class, args);
    }

    private static void makeChangeLog() throws IOException {
        File file = new File("change-log.log");
        if(!file.exists()){
            file.createNewFile();
        }
    }

}
