# Follow Service

## Overview
The Follow Service manages user relationships and social connections in the ConnectSphere platform. It handles following/unfollowing users, managing follower/following lists, and related social features.

## Features
- Follow/unfollow users
- Follower and following counts
- Follower/following lists
- Mutual follow detection
- Follow suggestions
- Privacy settings for follows
- Follow analytics

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
- **Port**: 8085 (default)
- Configuration files: `application.yml`

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Follow Operations
- `POST /api/v1/follows/{userId}` - Follow/unfollow user
- `GET /api/v1/follows/{userId}/followers` - Get user's followers
- `GET /api/v1/follows/{userId}/following` - Get users followed by user
- `GET /api/v1/follows/{userId}/status` - Check follow status

### Follow Statistics
- `GET /api/v1/follows/{userId}/counts` - Get follower/following counts
- `GET /api/v1/follows/user/suggestions` - Get follow suggestions

## Database Schema
- **Follow Entity**: Follower ID, following ID, timestamp

## Integration
- User validation via Auth Service
- Notification triggers for follows
- Feed updates based on follows
- Social analytics integration

## Security
- Authenticated user operations
- Privacy controls
- Rate limiting

## Features
- Bidirectional follow relationships
- Follow request system (if private accounts)
- Follow activity feeds

## Testing
- Comprehensive test coverage
- Integration tests
- JaCoCo code coverage

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
1. Maintain data consistency
2. Implement proper validation
3. Add comprehensive tests
4. Handle edge cases in relationships