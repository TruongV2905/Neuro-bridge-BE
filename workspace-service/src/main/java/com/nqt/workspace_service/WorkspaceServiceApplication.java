package com.nqt.workspace_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = "com.nqt")
@EnableFeignClients
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {
                "com.nqt.workspace_service",
                "com.nqt.common_starter"
        },
        exclude = {
                net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
        }
)
public class WorkspaceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkspaceServiceApplication.class, args);
	}

}