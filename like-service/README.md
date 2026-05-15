# Like Service

## Overview
The Like Service manages all like-related operations across the ConnectSphere platform. It handles liking/unliking posts, comments, and other content, as well as tracking like counts and user interactions.

## Features
- Like/unlike posts and comments
- Like count tracking and statistics
- User like history and preferences
- Real-time like updates
- Like analytics and reporting
- Bulk like operations

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with JPA/Hibernate
- **Messaging**: RabbitMQ (AMQP)
- **Service Discovery**: Eureka Client
- **Inter-service Communication**: OpenFeign
- **Security**: Spring Security, JWT
- **Documentation**: OpenAPI/Swagger
- **Java Version**: 17

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL database
- RabbitMQ server

## Configuration
- **Port**: 8084 (default)
- Configuration files: `application.yml`

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Like Operations
- `POST /api/v1/likes/posts/{postId}` - Like/unlike post
- `POST /api/v1/likes/comments/{commentId}` - Like/unlike comment
- `GET /api/v1/likes/posts/{postId}/count` - Get post like count
- `GET /api/v1/likes/comments/{commentId}/count` - Get comment like count

### User Likes
- `GET /api/v1/likes/user/posts` - Get user's liked posts
- `GET /api/v1/likes/user/comments` - Get user's liked comments

## Database Schema
- **Like Entity**: User, content type, content ID, timestamp

## Integration
- Communicates with Post and Comment services
- Updates like counts in real-time
- Triggers notifications for likes
- Publishes like events via RabbitMQ

## Security
- JWT-based authentication
- User-specific like operations
- Rate limiting for like actions

## Performance
- Optimized database queries
- Caching for like counts
- Asynchronous event processing

## Testing
- Unit tests with JUnit
- Integration tests
- Code coverage with JaCoCo

## Dependencies
- Spring Boot Web Starter
- Spring Data JPA
- Spring AMQP
- Spring Cloud OpenFeign
- Spring Security
- JWT libraries
- MySQL Connector
- Lombok

## Contributing
1. Ensure atomic like operations
2. Implement proper concurrency handling
3. Add validation for like actions
4. Write comprehensive tests