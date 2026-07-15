package com.nqt.rate_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.nqt.rate_service", "com.nqt.common_starter"})
@EnableDiscoveryClient
public class RateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RateServiceApplication.class, args);
    }
}