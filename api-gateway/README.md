# API Gateway

## Overview
The API Gateway serves as the single entry point for all client requests in the ConnectSphere microservices architecture. It handles routing, authentication, rate limiting, and cross-cutting concerns like CORS and logging.

## Features
- Request routing to appropriate microservices
- JWT token validation and authentication
- CORS configuration for frontend integration
- Load balancing across service instances
- Rate limiting and security filters
- API documentation aggregation

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Gateway**: Spring Cloud Gateway (WebFlux)
- **Service Discovery**: Netflix Eureka Client
- **Security**: JWT, Spring Security
- **Database**: Redis (for token blacklist)
- **Java Version**: 17

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- Redis server running

## Configuration
- **Port**: 8080
- Configuration file: `src/main/resources/application.yml`
- Environment variables required:
  - `JWT_SECRET`: Secret key for JWT validation
  - Redis connection settings

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints
All requests are routed through the gateway:
- **Base URL**: http://localhost:8080
- **Auth endpoints**: `/api/v1/auth/*`
- **Posts**: `/api/v1/posts/*`
- **Comments**: `/api/v1/comments/*`
- **Notifications**: `/api/v1/notifications/*`
- **Follows**: `/api/v1/follows/*`
- **Likes**: `/api/v1/likes/*`
- **Media**: `/api/v1/media/*`, `/api/v1/stories/*`
- **Search**: `/api/v1/search/*`, `/api/v1/hashtags/*`
- **Reports**: `/api/v1/report/*`
- **Payments**: `/api/v1/payment/*`

## Security
- JWT-based authentication for protected routes
- Token blacklist using Redis
- CORS enabled for frontend (localhost:5173)

## Dependencies
- Spring Cloud Gateway
- Spring Cloud Eureka Client
- Spring Boot Data Redis Reactive
- JWT libraries
- Lombok

## Monitoring
- Spring Boot Actuator endpoints available
- Gateway routes can be inspected via actuator

## Contributing
1. Update route configurations in `application.yml` for new services
2. Implement custom filters in the gateway for new requirements
3. Maintain security best practices
