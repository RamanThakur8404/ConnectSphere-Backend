# Report Service

## Overview
The Report Service provides comprehensive reporting and analytics capabilities for the ConnectSphere platform. It includes AI-powered analysis for content moderation, user behavior analytics, and system performance reporting.

## Features
- Content reporting and moderation
- AI-powered content analysis
- User behavior analytics
- System performance metrics
- Automated reporting
- Dashboard data aggregation
- Report generation and export
- Real-time analytics

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with JPA/Hibernate
- **AI Integration**: External AI API (HTTP client)
- **Messaging**: RabbitMQ (AMQP)
- **Service Discovery**: Eureka Client
- **Inter-service Communication**: OpenFeign
- **Security**: Spring Security, JWT
- **Documentation**: OpenAPI/Swagger
- **Monitoring**: Spring Boot Actuator
- **Java Version**: 17

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL database
- RabbitMQ server
- External AI service API access

## Configuration
- **Port**: 8090 (default)
- Configuration files: `application.yml`
- AI service API endpoints and keys

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Content Reports
- `POST /api/v1/report/content` - Report content
- `GET /api/v1/report/content/{id}` - Get report details
- `PUT /api/v1/report/content/{id}/status` - Update report status

### Analytics
- `GET /api/v1/report/analytics/users` - User analytics
- `GET /api/v1/report/analytics/content` - Content analytics
- `GET /api/v1/report/analytics/system` - System metrics

### AI Analysis
- `POST /api/v1/report/analyze` - AI content analysis
- `GET /api/v1/report/analysis/{id}` - Get analysis results

### Admin Reports
- `GET /api/v1/report/admin/dashboard` - Admin dashboard data
- `GET /api/v1/report/admin/export` - Export reports

## Database Schema
- **Report Entity**: Report details, status, analysis
- **AnalyticsData Entity**: Aggregated metrics
- **AIAnalysis Entity**: AI analysis results

## AI Integration
- Content moderation using AI
- Sentiment analysis
- Spam detection
- Image recognition
- Automated decision making

## Features
- Real-time report processing
- Automated moderation workflows
- Custom report templates
- Scheduled report generation
- Multi-format export (PDF, CSV, JSON)

## Security
- Role-based access control
- Sensitive data protection
- Audit logging
- API rate limiting

## Performance
- Asynchronous processing
- Data aggregation optimization
- Caching strategies
- Scalable architecture

## Testing
- AI integration testing
- Report accuracy validation
- Performance testing
- Code coverage with JaCoCo

## Dependencies
- Spring Boot Web Starter
- Spring Data JPA
- Spring AMQP
- Spring Cloud OpenFeign
- Spring Security
- JWT libraries
- Apache HTTP Client
- Jackson for JSON
- MySQL Connector
- Lombok

## Contributing
1. Improve AI analysis algorithms
2. Add new report types
3. Enhance analytics features
4. Implement advanced filtering