package com.connectsphere.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConnectsphereApplication {
	public static void main(String[] args) {
		SpringApplication.run(ConnectsphereApplication.class, args);
	}

}
