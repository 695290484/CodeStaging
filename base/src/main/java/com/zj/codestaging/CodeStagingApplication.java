package com.zj.codestaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CodeStagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeStagingApplication.class, args);
    }

}
