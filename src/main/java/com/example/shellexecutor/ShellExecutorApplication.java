package com.example.shellexecutor;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDubbo
@EnableScheduling
@SpringBootApplication
public class ShellExecutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShellExecutorApplication.class, args);
    }
} 