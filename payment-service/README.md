# Payment Service

## Overview
The Payment Service handles all payment processing and financial transactions in the ConnectSphere platform. It integrates with payment gateways to enable secure payments for premium features, subscriptions, and other monetization aspects.

## Features
- Payment gateway integration (Razorpay)
- Secure payment processing
- Subscription management
- Transaction history
- Refund processing
- Webhook handling
- Payment analytics
- Idempotency for payments

## Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Database**: MySQL with JPA/Hibernate
- **Payment Gateway**: Razorpay SDK
- **Cache**: Redis (for idempotency)
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
- Redis server
- RabbitMQ server
- Razorpay account and API keys

## Configuration
- **Port**: 8089 (default)
- Configuration files: `application.yml`
- Environment variables:
  - Razorpay API keys
  - Database and Redis settings

## Running the Service
```bash
mvn spring-boot:run
```

## API Endpoints

### Payment Operations
- `POST /api/v1/payments/create` - Create payment order
- `GET /api/v1/payments/{id}` - Get payment details
- `GET /api/v1/payments/user` - Get user payment history

### Webhooks
- `POST /api/v1/payments/webhook` - Razorpay webhook handler

### Subscription
- `POST /api/v1/payments/subscription` - Create subscription
- `GET /api/v1/payments/subscription/{id}` - Get subscription details

## Database Schema
- **Payment Entity**: Order details, status, user
- **Transaction Entity**: Payment transactions
- **Subscription Entity**: Subscription information

## Security
- Secure webhook signature verification
- Idempotency keys to prevent duplicates
- PCI compliance considerations
- User authentication required

## Integration
- User validation via Auth Service
- Notification triggers for payments
- Analytics for payment data
- Event publishing for payment events

## Features
- Multiple payment methods
- Currency support
- Tax calculation
- Invoice generation
- Payment retry logic

## Testing
- Mock payment testing
- Webhook simulation
- Integration tests
- Code coverage with JaCoCo

## Dependencies
- Spring Boot Web Starter
- Spring Data JPA
- Razorpay Java SDK
- Spring Data Redis
- Spring AMQP
- Spring Cloud OpenFeign
- Spring Security
- JWT libraries
- MySQL Connector
- Lombok

## Contributing
1. Add support for new payment methods
2. Implement subscription features
3. Add payment analytics
4. Ensure security best practices