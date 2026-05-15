# Post Service

## Overview
The Post Service manages all post-related operations in the ConnectSphere social media platform. It handles creating, reading, updating, and deleting posts, as well as managing post interactions and content.

## Features
- Create, read, update, delete posts
- Post content management (text, media)
- Post visibility and privacy settings
- Timeline and feed generation
- Post search and filtering
- Like and interaction tracking
- Comment integration
- Hashtag extraction and management

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
- **Port**: 8082 (default)
- Configuration files: `application.yml`
- Environment variables for database and messaging

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Post Management
- `GET /api/v1/posts` - Get posts feed
- `GET /api/v1/posts/{id}` - Get specific post
- `POST /api/v1/posts` - Create new post
- `PUT /api/v1/posts/{id}` - Update post
- `DELETE /api/v1/posts/{id}` - Delete post

### Post Interactions
- `GET /api/v1/posts/{id}/likes` - Get post likes
- `POST /api/v1/posts/{id}/like` - Like/unlike post
- `GET /api/v1/posts/user/{userId}` - Get user's posts

### Search and Filtering
- `GET /api/v1/posts/search` - Search posts
- `GET /api/v1/posts/hashtag/{hashtag}` - Posts by hashtag

## Database Schema
- **Post Entity**: Content, author, timestamps, visibility
- **Like Entity**: User-post relationships
- **Hashtag Entity**: Tag management

## Integration
- Communicates with Auth Service for user validation
- Integrates with Media Service for file uploads
- Uses RabbitMQ for event-driven updates
- Connects with Search Service for indexing

## Security
- JWT-based authentication
- User authorization for post operations
- Input validation and sanitization

## Asynchronous Processing
- Event publishing for post creation/updates
- Background processing for media handling
- Notification triggers for interactions

## Testing
- Unit tests with JUnit and Mockito
- Integration tests with TestContainers
- Code coverage with JaCoCo (80% minimum)

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
- Health endpoints via Actuator
- Logging with AOP aspects
- Performance metrics

## Contributing
1. Follow REST API conventions
2. Implement proper error handling
3. Add validation for all inputs
4. Write comprehensive tests
5. Update OpenAPI documentation