# Auth Service

## Overview
The Authentication Service handles user registration, login, OAuth2 authentication, password management, and JWT token operations for the ConnectSphere platform. It provides secure user authentication and authorization services.

## Features
- User registration and login
- JWT token generation and validation
- OAuth2 integration (Google)
- Password reset via email
- User profile management
- Token refresh functionality
- User deactivation
- Public user search

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security, JWT
- **Database**: MySQL with JPA/Hibernate
- **Messaging**: RabbitMQ (AMQP)
- **Email**: SMTP (JavaMail)
- **Cache**: Redis
- **Service Discovery**: Eureka Client
- **Documentation**: OpenAPI/Swagger
- **Java Version**: 17

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL database
- Redis server
- RabbitMQ server
- SMTP server (Gmail recommended)

## Configuration
- **Port**: 8081 (default)
- Configuration files: `application.properties`
- Environment variables required:
  - Database connection settings
  - JWT_SECRET
  - Redis connection
  - Mail server settings (MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD)
  - OAuth2 client credentials

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Public Endpoints
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/forget-password` - Request password reset
- `POST /api/v1/auth/reset-password` - Reset password
- `GET /api/v1/auth/users/public/search` - Search public users

### Protected Endpoints
- `GET /api/v1/auth/profile` - Get user profile
- `PUT /api/v1/auth/profile` - Update profile
- `PUT /api/v1/auth/password` - Change password
- `DELETE /api/v1/auth/deactivate` - Deactivate account

### OAuth2 Endpoints
- `/oauth2/authorization/google` - Google login
- `/login/oauth2/code/*` - OAuth2 callbacks

## Database Schema
- **User Entity**: Stores user information, credentials, OAuth2 data
- **Token Blacklist**: Redis-based token invalidation

## Security Features
- Password encryption with BCrypt
- JWT token-based authentication
- Token expiration and refresh
- Rate limiting for auth endpoints
- Input validation and sanitization

## Email Functionality
- Password reset emails with secure tokens
- HTML email templates
- SMTP configuration for reliable delivery

## Testing
- Unit tests with JUnit 5 and Mockito
- Integration tests with H2 in-memory database
- Code coverage reporting with JaCoCo (80% minimum)

## Dependencies
- Spring Boot Web Starter
- Spring Security
- Spring Data JPA
- Spring AMQP
- Spring Mail
- Spring Data Redis
- JWT libraries
- MySQL Connector
- Lombok
- SpringDoc OpenAPI

## Monitoring
- Health checks via Spring Boot Actuator
- Metrics and logging
- Swagger UI at `/swagger-ui.html`

## Contributing
1. Follow security best practices
2. Implement proper input validation
3. Add comprehensive tests
4. Update API documentation
5. Maintain backward compatibility
