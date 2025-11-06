# Microservices E-Commerce System

## Architecture Overview

```mermaid
graph TB
    AG[API Gateway] -->|REST + Resilience| OS[Order Service]
    AG[API Gateway] -->|REST + Resilience| IS[Inventory Service]
    OS -->|gRPC + Resilience| IS[Inventory Service]
    OS -->|Kafka Async| NS[Notification Service]
    
    OS --> MD[(MongoDB)]
    IS --> PD[(PostgreSQL)]
```

## Services

**API Gateway**: Routes requests to Order Service (REST + Resilience4j)

**Order Service**: Processes orders, uses MongoDB for flexible order data

**Inventory Service**: Manages stock levels, uses PostgreSQL for ACID compliance and complex query.

**Notification Service**: Handles async notifications via Kafka

## Communication Patterns

**REST**: API Gateway → Order Service (with circuit breaker)

**gRPC**: Order Service → Inventory Service (high performance)

**Kafka**: Order Service → Notification Service (async events)

## Database Choice

**MongoDB for Orders**: Document structure fits nested order items

**PostgreSQL for Inventory**: Strong consistency for stock management

# To start
## (Optional) Local Docker Compose Setup
### Start all services
```bash
docker compose -p zeli-lab4 up -d
```
### clean up all services
```bash
docker compose -p zeli-lab4 down --rmi all
```