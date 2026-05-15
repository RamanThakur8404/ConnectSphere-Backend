# Media Service

## Overview
The Media Service handles all media-related operations in the ConnectSphere platform, including file uploads, storage, processing, and delivery of images, videos, and other media content.

## Features
- File upload and storage
- Image processing and optimization
- Video streaming and transcoding
- Media metadata extraction
- CDN integration
- Media access control
- Story management
- Media analytics

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with JPA/Hibernate
- **File Storage**: Local filesystem / Cloud storage
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
- File storage system

## Configuration
- **Port**: 8087 (default)
- Configuration files: `application.yml`
- Storage path configurations

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Media Upload
- `POST /api/v1/media/upload` - Upload media file
- `POST /api/v1/media/upload/multiple` - Upload multiple files
- `POST /api/v1/stories/upload` - Upload story

### Media Retrieval
- `GET /api/v1/media/{id}` - Get media file
- `GET /api/v1/media/{id}/thumbnail` - Get thumbnail
- `GET /api/v1/media/{id}/metadata` - Get media metadata

### Media Management
- `DELETE /api/v1/media/{id}` - Delete media
- `PUT /api/v1/media/{id}` - Update media metadata

### Stories
- `GET /api/v1/stories` - Get user stories
- `GET /api/v1/stories/{id}` - Get specific story
- `DELETE /api/v1/stories/{id}` - Delete story

## Database Schema
- **Media Entity**: File info, metadata, user association
- **Story Entity**: Story content, expiration

## Features
- Multiple file format support
- Image resizing and compression
- Video format conversion
- Access control and permissions
- Media moderation
- Bandwidth optimization

## Integration
- Used by Post Service for attachments
- Profile pictures via Auth Service
- CDN for global delivery
- Virus scanning integration

## Security
- File type validation
- Size limits
- User authentication
- Access token validation

## Performance
- Asynchronous processing
- Caching headers
- Optimized storage
- CDN integration

## Testing
- File upload testing
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
1. Add support for new media types
2. Implement processing optimizations
3. Add security validations
4. Write comprehensive tests