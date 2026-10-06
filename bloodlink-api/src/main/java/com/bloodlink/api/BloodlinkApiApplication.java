package com.bloodlink.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BloodlinkApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BloodlinkApiApplication.class, args);
    }
}
