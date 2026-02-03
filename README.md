# E-Commerce Microservices Platform

A production-ready, event-driven microservices architecture for e-commerce applications built with Spring Boot, Kafka, and PostgreSQL.

## 🚀 Quick Start
**👉 [Read the LOCAL SETUP GUIDE (LOCAL_SETUP.md)](LOCAL_SETUP.md) for full instructions.**

For the impatient:
```bash
./start.sh
```

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Services Deep Dive](#services-deep-dive)
4. [Domain Models](#domain-models)
5. [Event Flow](#event-flow)
6. [API Reference](#api-reference)
7. [Getting Started](#getting-started)
8. [Configuration](#configuration)
9. [Deployment](#deployment)
10. [Monitoring](#monitoring)

---

## System Overview

This platform provides a complete e-commerce backend with:

- **User Management**: Registration, authentication, profile management
- **Order Processing**: Order creation, tracking, and lifecycle management
- **Payment Processing**: Secure payment handling with idempotency
- **Notification System**: Email notifications for all major events
- **Event-Driven Architecture**: Asynchronous communication via Kafka
- **Outbox Pattern**: Reliable event publishing with exactly-once semantics

### Technology Stack

| Component | Technology |
|-----------|------------|
| API Gateway | Spring Cloud Gateway |
| Microservices | Spring Boot 3.2.5 |
| Message Broker | Apache Kafka |
| Databases | PostgreSQL 16 |
| Caching | Redis (optional) |
| Security | JWT (JJWT 0.12.5) |
| Monitoring | Spring Boot Actuator, Micrometer |
| Resilience | Resilience4j (Circuit Breaker, Retry, Bulkhead) |

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Application                         │
│                    (Web App / Mobile App / API Consumer)              │
└──────────────────────────┬─────────────────────────────────────────┘
                           │ HTTPS / REST
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Port 8080)                       │
│  ┌─────────────────────────────────────────────────────────────┐     │
│  │  • JWT Authentication & Validation                          │     │
│  │  • Request Routing to Services                              │     │
│  │  • Rate Limiting (60 req/min default)                       │     │
│  │  • CORS Configuration                                       │     │
│  │  • Security Headers (CSP, HSTS, XSS Protection)             │     │
│  │  • Correlation ID Tracking                                  │     │
│  └─────────────────────────────────────────────────────────────┘     │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│  User Service │  │ Order Service │  │Payment Service│
│   Port 8081   │  │   Port 8082   │  │   Port 8083   │
│               │  │               │  │               │
│ • Register    │  │ • Create      │  │ • Process     │
│ • Login       │  │ • Track       │  │ • Verify      │
│ • Profile     │  │ • Manage      │  │ • Idempotency │
└───────┬───────┘  └───────┬───────┘  └───────┬───────┘
        │                  │                  │
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│   User DB     │  │   Order DB    │  │  Payment DB   │
│  Port 5433    │  │  Port 5434    │  │  Port 5435    │
│  PostgreSQL   │  │  PostgreSQL   │  │  PostgreSQL   │
└───────────────┘  └───────────────┘  └───────────────┘
        │                  │                  │
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │ Events via Kafka
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Topics:                                                     │   │
│  │  • user-events (Partitions: 3, Replicas: 1)                  │   │
│  │  • order-events (Partitions: 3, Replicas: 1)               │   │
│  │  • payment-events (Partitions: 3, Replicas: 1)             │   │
│  │  • order-events.dlq (Dead Letter Queue)                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Notification Service (Port 8084)                   │
│  ┌─────────────────────────────────────────────────────────────┐     │
│  │  Consumes events and sends:                                  │     │
│  │  • Welcome emails (UserCreated)                              │     │
│  │  • Order confirmations (OrderCreated)                        │     │
│  │  • Payment receipts (PaymentProcessed)                       │     │
│  └─────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

### Communication Patterns

1. **Synchronous**: REST API calls via API Gateway (for client requests)
2. **Asynchronous**: Kafka events (for service-to-service communication)
3. **Outbox Pattern**: Database + Poller for reliable event publishing

---

## Services Deep Dive

### 1. API Gateway (Port 8080)

**Purpose**: Single entry point for all client requests

**Responsibilities**:
- JWT token validation on protected routes
- Route requests to appropriate services
- Add user context headers (X-User-Id, X-User-Role, X-User-Email)
- Rate limiting (configurable per endpoint)
- CORS handling
- Security headers

**Routes**:
```
/api/users/register → User Service (Public)
/api/users/login    → User Service (Public)
/api/users/**       → User Service (Protected)
/api/orders/**      → Order Service (Protected)
/api/payments/**    → Payment Service (Protected)
```

**Configuration**:
```yaml
security:
  rate-limit:
    requests-per-minute: 60
  max-request-size: 1048576  # 1MB
```

---

### 2. User Service (Port 8081)

**Purpose**: User registration, authentication, and profile management

**Database**: PostgreSQL (user_db, Port 5433)

**Entities**:

#### User
```java
@Entity
@Table(name = "users")
public class User {
    UUID id;                    // Primary key
    String email;               // Unique, indexed
    String password;            // BCrypt hashed
    String fullName;
    Role role;                  // USER or ADMIN
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

**API Endpoints**:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/users/register | Public | Register new user |
| POST | /api/users/login | Public | Authenticate user |
| GET | /api/users/profile | Protected | Get current user profile |

**Events Published**:
- `UserCreated` → user-events topic

**Outbox Pattern**:
- Events saved to `outbox_events` table
- Poller runs every 100ms to publish to Kafka
- Guarantees exactly-once event publishing

---

### 3. Order Service (Port 8082)

**Purpose**: Order creation, tracking, and lifecycle management

**Database**: PostgreSQL (order_db, Port 5434)

**Resilience Patterns**:
- Circuit Breaker for database operations
- Retry with exponential backoff
- Bulkhead for concurrency limiting
- Timeout configuration

**Entities**:

#### Order
```java
@Entity
@Table(name = "orders")
public class Order {
    UUID id;                    // Primary key
    UUID userId;                // Who placed the order
    String userEmail;           // For notifications
    BigDecimal totalAmount;     // Calculated from items
    OrderStatus status;         // CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    List<OrderItem> items;      // One-to-many relationship
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

#### OrderItem
```java
@Entity
@Table(name = "order_items")
public class OrderItem {
    UUID id;
    Order order;                // Many-to-one
    UUID productId;
    Integer quantity;
    BigDecimal price;           // Price at time of order
}
```

**Order Status Flow**:
```
CREATED → PAID → SHIPPED → DELIVERED
    ↓
CANCELLED
```

**API Endpoints**:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/orders | Protected | Create new order |
| GET | /api/orders/{id} | Protected | Get order details |
| GET | /api/orders | Protected | List user's orders (paginated) |

**Events**:
- **Published**: `OrderCreated` → order-events topic
- **Consumed**: `PaymentCompletedEvent` from payment-events topic

**Caching**:
- Order details cached for 10 minutes
- Order lists cached for 1 minute
- Redis support (optional, configurable)

---

### 4. Payment Service (Port 8083)

**Purpose**: Payment processing with idempotency guarantees

**Database**: PostgreSQL (payment_db, Port 5435)

**Entities**:

#### Payment
```java
@Entity
@Table(name = "payments")
public class Payment {
    UUID id;
    UUID orderId;               // Links to order
    BigDecimal amount;
    PaymentStatus status;       // PENDING, COMPLETED, FAILED
    String transactionId;       // External payment gateway ID
    String userEmail;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

#### IdempotentRequest
```java
@Entity
@Table(name = "idempotent_requests")
public class IdempotentRequest {
    UUID id;
    String idempotencyKey;      // Unique client-provided key
    String requestHash;         // Hash of request content
    String requestType;         // e.g., "PROCESS_PAYMENT"
    String responsePayload;     // Cached response
    Integer responseStatusCode;
    Instant expiresAt;          // Auto-cleanup after 24 hours
}
```

**API Endpoints**:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/payments/process | Protected | Process payment for order |
| GET | /api/payments/order/{orderId} | Protected | Get payment status |

**Events**:
- **Published**: `PaymentProcessed` → payment-events topic
- **Consumed**: `OrderCreated` from order-events topic

**Idempotency**:
- Client provides `X-Idempotency-Key` header
- Same key + request = same response (24h window)
- Prevents duplicate charges

---

### 5. Notification Service (Port 8084)

**Purpose**: Send email notifications for business events

**Database**: None (stateless)

**Events Consumed**:

| Event | Topic | Action |
|-------|-------|--------|
| UserCreated | user-events | Send welcome email |
| OrderCreated | order-events | Send order confirmation |
| PaymentProcessed | payment-events | Send payment receipt |

**Configuration**:
```yaml
spring:
  kafka:
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest
```

---

## Domain Models

### Product Catalog (Simplified)

Products are referenced by ID across services (product data managed separately):

```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Wireless Headphones",
  "price": 99.99,
  "currency": "USD",
  "sku": "WH-001"
}
```

### Order Creation Request

```json
{
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 99.99
    },
    {
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 1,
      "price": 49.99
    }
  ]
}
```

### Order Response

```json
{
  "id": "order-uuid",
  "userId": "user-uuid",
  "userEmail": "customer@example.com",
  "totalAmount": 249.97,
  "status": "CREATED",
  "items": [
    {
      "id": "item-uuid",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 99.99,
      "subtotal": 199.98
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

## Event Flow

### 1. User Registration Flow

```
Client → API Gateway → User Service
                          │
                          │ (Transaction)
                          ▼
                   ┌──────────────┐
                   │   User DB    │
                   └──────────────┘
                          │
                          │ (Outbox Pattern)
                          ▼
                   ┌──────────────┐
                   │ Outbox Table │
                   └──────────────┘
                          │
                          │ (Poller)
                          ▼
                   ┌──────────────┐
                   │ Kafka Topic  │
                   │  user-events │
                   └──────────────┘
                          │
                          ▼
                   ┌──────────────┐
                   │ Notification │
                   │   Service    │
                   └──────────────┘
                          │
                          ▼
                   Send Welcome Email
```

### 2. Order Creation Flow

```
Client → API Gateway → Order Service
                          │
                          │ (Transaction: Order + Outbox)
                          ▼
                   ┌──────────────┐     ┌──────────────┐
                   │   Order DB   │     │ Outbox Table │
                   └──────────────┘     └──────────────┘
                          │
                          │ (Poller)
                          ▼
                   ┌──────────────┐
                   │ Kafka Topic  │
                   │ order-events │
                   └──────────────┘
                          │
            ┌─────────────┴─────────────┐
            │                           │
            ▼                           ▼
    ┌──────────────┐          ┌──────────────┐
    │Payment Service│         │ Notification │
    └──────────────┘          │   Service    │
           │                  └──────────────┘
           │                         │
           ▼                         ▼
    Process Payment           Send Order
    (Auto-triggered)          Confirmation
```

### 3. Payment Processing Flow

```
Payment Service (consumed OrderCreated)
        │
        │ (Transaction)
        ▼
┌──────────────┐     ┌──────────────┐
│ Payment DB   │     │ Outbox Table │
└──────────────┘     └──────────────┘
        │
        │ (Poller)
        ▼
┌──────────────┐
│ Kafka Topic  │
│payment-events│
└──────────────┘
        │
        ▼
┌──────────────┐
│Order Service │ (updates order status)
└──────────────┘
        │
        ▼
┌──────────────┐
│ Notification │ (sends receipt)
└──────────────┘
```

---

## API Reference

### Authentication

All protected endpoints require JWT token in Authorization header:

```
Authorization: Bearer <jwt_token>
```

### User Service

#### Register User
```http
POST /api/users/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "fullName": "John Doe"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### Login
```http
POST /api/users/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response**: Same as Register

#### Get Profile
```http
GET /api/users/profile
Authorization: Bearer <token>
```

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00"
}
```

### Order Service

#### Create Order
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json
X-Idempotency-Key: <uuid>  # Optional

{
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

**Response**:
```json
{
  "id": "order-uuid",
  "userId": "user-uuid",
  "userEmail": "user@example.com",
  "totalAmount": 59.98,
  "status": "CREATED",
  "items": [...],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get Order
```http
GET /api/orders/{orderId}
Authorization: Bearer <token>
```

#### List Orders
```http
GET /api/orders?page=0&size=10
Authorization: Bearer <token>
```

### Payment Service

#### Get Payment by Order
```http
GET /api/payments/order/{orderId}
Authorization: Bearer <token>
```

**Response**:
```json
{
  "id": "payment-uuid",
  "orderId": "order-uuid",
  "amount": 59.98,
  "status": "COMPLETED",
  "transactionId": "txn_123456",
  "createdAt": "2024-01-15T10:31:00"
}
```

---

## Getting Started

### Prerequisites

- Docker 20.10+ with 8GB+ RAM
- Docker Compose 2.0+
- Maven 3.8+ (optional)
- Java 17+ (optional, for local development)

### Quick Start with Docker

```bash
# 1. Clone and navigate
cd e-commerce-microservices

# 2. Set environment variables
cp .env.example .env
# Edit .env and set secure JWT_SECRET

# 3. Build and start all services
docker-compose up -d --build

# 4. Wait for health checks (30-60 seconds)
docker-compose ps

# 5. Test the API
curl http://localhost:8080/actuator/health
```

### Local Development (Without Docker)

```bash
# 1. Build the project
mvn clean install -DskipTests

# 2. Start infrastructure (if using Docker for infra only)
docker-compose up -d zookeeper kafka user-db order-db payment-db

# 3. Set environment
export JWT_SECRET="your-32-char-secret-here"
export DB_PASSWORD="postgres"

# 4. Run services individually
cd user-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
# ... etc
```

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | (required) | 32+ character secret for JWT signing |
| `JWT_EXPIRATION` | 86400000 | Token expiration in ms (24h default) |
| `DB_URL` | jdbc:postgresql://localhost:5432/... | Database connection URL |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka broker addresses |
| `CACHE_TYPE` | simple | Cache provider (simple/redis) |
| `REDIS_HOST` | localhost | Redis hostname |
| `REDIS_PORT` | 6379 | Redis port |

### Service Ports

| Service | Port | Database Port |
|---------|------|---------------|
| API Gateway | 8080 | - |
| User Service | 8081 | 5433 |
| Order Service | 8082 | 5434 |
| Payment Service | 8083 | 5435 |
| Notification Service | 8084 | - |
| Kafka | 9092 | - |
| Zookeeper | 2181 | - |

---

## Deployment

### Docker Compose (Development)

```bash
# Start everything
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Scale order service
docker-compose up -d --scale order-service=3

# Stop everything
docker-compose down -v
```

### Production Considerations

1. **Security**:
   - Use strong JWT_SECRET (32+ chars)
   - Enable HTTPS/TLS
   - Set secure database passwords
   - Use secret management (Vault, AWS Secrets Manager)

2. **Database**:
   - Use `ddl-auto: validate` (never create-drop)
   - Run Flyway migrations manually
   - Enable connection pooling (HikariCP)
   - Set up read replicas

3. **Kafka**:
   - Use Kafka cluster (not single node)
   - Configure replication factor >= 3
   - Set up monitoring (Kafka Manager)
   - Enable SSL/SASL for security

4. **Caching**:
   - Enable Redis in production
   - Configure appropriate TTLs
   - Monitor cache hit rates

5. **Monitoring**:
   - Prometheus for metrics
   - Grafana for dashboards
   - ELK stack for logging
   - PagerDuty for alerts

---

## Monitoring

### Health Endpoints

All services expose actuator endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Circuit breaker status
curl http://localhost:8082/actuator/circuitbreakers

# Prometheus metrics
curl http://localhost:8082/actuator/prometheus
```

### Kafka Monitoring

```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:29092

# Topic details
docker-compose exec kafka kafka-topics --describe --topic order-events --bootstrap-server localhost:29092

# Consume messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic order-events \
  --from-beginning

# Check consumer groups
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --list
```

### Database Monitoring

```bash
# Connect to database
docker-compose exec user-db psql -U postgres -d user_db

# Check tables\dt

# View outbox events
SELECT * FROM outbox_events WHERE processed = false;
```

---

## Testing

### Integration Testing

```bash
# Run all tests
mvn test

# Run with Testcontainers
mvn verify

# Run specific service tests
cd order-service && mvn test
```

### Manual Testing

```bash
# 1. Register user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","fullName":"Test User"}'

# 2. Login (save token)
TOKEN=$(curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | jq -r '.token')

# 3. Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"productId":"prod-001","quantity":2,"price":29.99}]}'

# 4. Check notifications
docker-compose logs notification-service
```

---

## Troubleshooting

### Common Issues

**1. Services fail to start**
- Check database connection strings
- Verify JWT_SECRET is set and >= 32 chars
- Check Kafka bootstrap servers

**2. Kafka connection errors**
- Ensure Zookeeper is running before Kafka
- Check `KAFKA_ADVERTISED_LISTENERS` configuration
- Verify port 9092 is not in use

**3. Database connection refused**
- Check if database containers are healthy
- Verify database ports (5433, 5434, 5435)
- Check firewall settings

**4. Events not being published**
- Check outbox_events table for unprocessed events
- Verify KafkaTemplate bean is created
- Check Kafka topic exists

### Debug Mode

```bash
# Enable debug logging
export LOG_LEVEL=DEBUG

# Run with debug
mvn spring-boot:run -Dspring-boot.run.arguments=--debug
```

---

## Architecture Patterns Used

1. **Database-per-Service**: Each service owns its data
2. **API Gateway**: Single entry point with cross-cutting concerns
3. **Outbox Pattern**: Reliable event publishing
4. **Event Sourcing**: Order service tracks domain events
5. **Saga Pattern**: Distributed transaction coordination
6. **Circuit Breaker**: Fault tolerance (Resilience4j)
7. **CQRS**: Separate read/write models (optional)

---

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

## License

MIT License - See LICENSE file for details

---

## Support

For issues and questions:
- Create GitHub Issue
- Check existing documentation
- Review FIXES_APPLIED.md for recent changes
