# Notification Service

## Overview
The Notification Service handles all notification-related functionality in the ConnectSphere platform. It manages real-time notifications for user interactions, system events, and personalized alerts.

## Features
- Real-time push notifications
- Email notifications
- In-app notification center
- Notification preferences management
- Notification history and archiving
- Bulk notification sending
- Notification analytics

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
- Email service (SMTP)

## Configuration
- **Port**: 8086 (default)
- Configuration files: `application.yml`

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Notification Management
- `GET /api/v1/notifications` - Get user notifications
- `PUT /api/v1/notifications/{id}/read` - Mark notification as read
- `DELETE /api/v1/notifications/{id}` - Delete notification
- `PUT /api/v1/notifications/read-all` - Mark all as read

### Preferences
- `GET /api/v1/notifications/preferences` - Get notification preferences
- `PUT /api/v1/notifications/preferences` - Update preferences

### Admin
- `POST /api/v1/notifications/broadcast` - Send broadcast notification

## Database Schema
- **Notification Entity**: User, type, content, status, timestamp
- **NotificationPreference Entity**: User preferences

## Integration
- Receives events from all services via RabbitMQ
- User data from Auth Service
- Real-time delivery via WebSocket (future)
- Email service integration

## Notification Types
- Like notifications
- Comment notifications
- Follow notifications
- Post interaction notifications
- System announcements
- Friend requests

## Security
- User-specific notifications
- Authentication required
- Content filtering

## Performance
- Asynchronous processing
- Message queuing
- Batch operations
- Caching strategies

## Testing
- Event-driven testing
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
1. Implement new notification types
2. Add preference controls
3. Optimize delivery mechanisms
4. Write comprehensive tests