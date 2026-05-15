package com.connectsphere.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Main entry point for the ConnectSphere Eureka Naming Server.
@SpringBootApplication
@EnableEurekaServer // This single annotation converts a standard Spring Boot application into a Eureka Service Registry
public class EurekaServerApplication {	
    public static void main(String[] args) {
        // Bootstraps the application, starting the embedded Tomcat server 
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
