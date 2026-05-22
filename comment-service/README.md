# Comment Service

## Overview
The Comment Service manages all comment-related functionality for posts in the ConnectSphere platform. It handles creating, retrieving, updating, and deleting comments, as well as managing comment threads and interactions.

## Features
- Create and manage comments on posts
- Nested comment threads (replies)
- Comment editing and deletion
- Comment liking and interaction tracking
- Comment moderation and reporting
- Real-time comment updates
- Comment search and filtering

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
- **Port**: 8083 (default)
- Configuration files: `application.yml`
- Database and messaging configurations

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Comment Management
- `GET /api/v1/comments/post/{postId}` - Get comments for a post
- `POST /api/v1/comments` - Create new comment
- `PUT /api/v1/comments/{id}` - Update comment
- `DELETE /api/v1/comments/{id}` - Delete comment

### Comment Interactions
- `POST /api/v1/comments/{id}/like` - Like/unlike comment
- `GET /api/v1/comments/{id}/replies` - Get comment replies
- `POST /api/v1/comments/{id}/reply` - Reply to comment

### Comment Search
- `GET /api/v1/comments/search` - Search comments

## Database Schema
- **Comment Entity**: Content, author, post reference, parent comment
- **CommentLike Entity**: User-comment relationships

## Integration
- Validates users through Auth Service
- Updates post comment counts via Post Service
- Sends notifications via Notification Service
- Publishes events via RabbitMQ

## Security
- JWT authentication required
- User authorization for comment operations
- Content moderation checks

## Asynchronous Features
- Event-driven notification system
- Background processing for moderation
- Real-time updates via messaging

## Testing
- Comprehensive unit and integration tests
- H2 in-memory database for testing
- JaCoCo code coverage (80% minimum)

## Dependencies
- Spring Boot Web Starter
- Spring Data JPA
- Spring AMQP
- Spring Cloud OpenFeign
- Spring Security
- JWT libraries
- MySQL Connector
- Lombok
- SpringDoc OpenAPI

## Monitoring
- Health checks and metrics
- AOP-based logging
- Performance monitoring

## Contributing
1. Maintain thread safety for comment operations
2. Implement proper validation
3. Add comprehensive error handling
4. Write thorough tests
5. Update API documentation