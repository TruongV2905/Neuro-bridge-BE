package com.nqt.jobPost_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.nqt.jobPost_service",
        "com.nqt.common_starter"
})
public class JobPostServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobPostServiceApplication.class, args);
	}

}
