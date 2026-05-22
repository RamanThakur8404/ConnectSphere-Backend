# Eureka Server

## Overview
The Eureka Server is a service registry that enables service discovery in the ConnectSphere microservices architecture. It allows microservices to register themselves and discover other services dynamically, eliminating the need for hardcoded service URLs.

## Features
- Service registration and discovery
- Health monitoring of registered services
- Load balancing support
- High availability configuration

## Technology Stack
- **Framework**: Spring Boot 3.2.4
- **Service Discovery**: Netflix Eureka Server
- **Java Version**: 17

## Prerequisites
- Java 17 or higher
- Maven 3.6+

## Configuration
The server runs on port 8761 by default. Configuration can be found in `src/main/resources/application.yml`.

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints
- **Eureka Dashboard**: http://localhost:8761
- **Service Registry**: http://localhost:8761/eureka/apps

## Dependencies
- Spring Cloud Netflix Eureka Server
- Spring Boot Starter Test

## Health Check
The service provides health check endpoints via Spring Boot Actuator.

## Contributing
1. Follow the existing code structure
2. Write unit tests for new features
3. Ensure code coverage is maintained above 80%