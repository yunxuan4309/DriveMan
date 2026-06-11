package com.homework.driveman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 驾校报名管理系统 — 启动入口
 */
@SpringBootApplication
@EnableScheduling
public class DriveManApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriveManApplication.class, args);
    }

}
