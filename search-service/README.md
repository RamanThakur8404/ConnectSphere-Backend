# Search Service

## Overview
The Search Service provides comprehensive search and indexing capabilities for the ConnectSphere platform. It enables users to search for posts, users, hashtags, and other content with advanced filtering and ranking.

## Features
- Full-text search across posts
- User search and discovery
- Hashtag search and trending
- Advanced filtering options
- Search result ranking
- Search analytics
- Autocomplete suggestions
- Search history

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with JPA/Hibernate
- **Search Engine**: Elasticsearch (recommended)
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
- Elasticsearch (optional, for advanced search)

## Configuration
- **Port**: 8088 (default)
- Configuration files: `application.yml`

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Search Operations
- `GET /api/v1/search/posts` - Search posts
- `GET /api/v1/search/users` - Search users
- `GET /api/v1/search` - General search

### Hashtag Operations
- `GET /api/v1/hashtags/trending` - Get trending hashtags
- `GET /api/v1/hashtags/{tag}` - Posts by hashtag
- `GET /api/v1/hashtags/search` - Search hashtags

### Advanced Search
- `GET /api/v1/search/advanced` - Advanced search with filters
- `GET /api/v1/search/suggestions` - Autocomplete suggestions

## Database Schema
- **SearchIndex Entity**: Indexed content
- **Hashtag Entity**: Hashtag tracking
- **SearchHistory Entity**: User search history

## Features
- Real-time indexing
- Relevance scoring
- Faceted search
- Geospatial search (future)
- Personalized results
- Search result caching

## Integration
- Indexes content from Post Service
- User data from Auth Service
- Real-time updates via RabbitMQ
- Analytics integration

## Performance
- Optimized indexing
- Query caching
- Load balancing
- Scalable architecture

## Security
- Search result filtering
- User privacy controls
- Rate limiting

## Testing
- Search query testing
- Index accuracy tests
- Performance tests
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
1. Improve search algorithms
2. Add new search filters
3. Optimize indexing performance
4. Implement advanced search features