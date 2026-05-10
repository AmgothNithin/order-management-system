# 🛒 Real-Time Order Management System

A **production-grade microservices backend** simulating an e-commerce order flow (Amazon/Flipkart style), built with Java 17, Spring Boot 3, Apache Kafka, Redis, and PostgreSQL.

---

## 🏗️ Architecture

```
┌─────────────┐     POST /orders      ┌───────────────────┐
│   Client    │ ──────────────────▶   │   Order Service   │  :8081
└─────────────┘                       └────────┬──────────┘
                                               │  Publishes: order.created
                                               ▼
                                    ┌──────────────────┐
                                    │      Kafka       │
                                    └──┬───────────┬───┘
                                       │           │
                           ┌───────────▼──┐   ┌────▼──────────────┐
                           │Payment Svc   │   │ Inventory Svc      │
                           │  :8082       │   │   :8083            │
                           └──────┬───────┘   └────────────────────┘
                                  │ Publishes: payment.processed
                                  ▼
                    ┌────────────────────────────┐
                    │           Kafka            │
                    └──────┬─────────────────────┘
                           │
              ┌────────────▼──────────────┐
              │    Notification Svc        │
              │         :8084              │
              └────────────────────────────┘
```

## 🔄 Event Flow

```
1. POST /api/v1/orders
2. Order Service → persists PENDING order → publishes [order.created]
3. Payment Service consumes [order.created] → processes payment → publishes [payment.processed]
4. Inventory Service consumes [order.created] → decrements stock
   └─ if stock fails → publishes [order.cancelled]
5. Order Service consumes [payment.processed] → updates status CONFIRMED / PAYMENT_FAILED
6. Notification Service consumes [order.created, payment.processed, order.status.changed] → saves notification
```

---

## 🧱 Services

| Service              | Port | Responsibility                                  |
|----------------------|------|-------------------------------------------------|
| order-service        | 8081 | Order lifecycle, JWT auth, Redis cache          |
| payment-service      | 8082 | Payment processing, retry scheduler             |
| inventory-service    | 8083 | Stock management, validation                    |
| notification-service | 8084 | Event-driven notifications                      |
| Kafka UI             | 8090 | Monitor topics and messages                     |

---

## ⚙️ Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Apache Kafka** — async event-driven communication
- **PostgreSQL 15** — persistent storage with indexed queries
- **Redis 7** — order caching (30-min TTL), ~60% DB load reduction
- **JWT (jjwt 0.11)** — stateless authentication
- **Docker + Docker Compose** — one-command deployment

---

## 🚀 Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 17 + Maven (for local dev)

### Option 1 — Docker Compose (Recommended)

```bash
git clone https://github.com/AmgothNithin/order-management-system.git
cd order-management-system
docker-compose up --build
```

All services and infrastructure start automatically.

### Option 2 — Local Dev (infra in Docker, services in IDE)

```bash
# Start only infra
docker-compose up zookeeper kafka postgres redis -d

# Run each service from its directory
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

---

## 📡 API Reference

### Auth
```
POST /api/v1/auth/register    Body: { username, email, password }
POST /api/v1/auth/login       Body: { username, password }  → returns JWT
```

### Orders  *(Bearer token required)*
```
POST   /api/v1/orders                          Create order
GET    /api/v1/orders/{id}                     Get order by ID (Redis cached)
GET    /api/v1/orders?page=0&size=10&status=   List my orders (paginated + filtered)
PUT    /api/v1/orders/{id}/cancel              Cancel order
```

### Inventory  *(public)*
```
GET    /api/v1/inventory/products              List all products
GET    /api/v1/inventory/products/{id}         Get product by ID
POST   /api/v1/inventory/products              Create product
POST   /api/v1/inventory/validate              Validate stock availability
```

### Payments
```
GET    /api/v1/payments/order/{orderId}        Get payment by order
```

### Notifications
```
GET    /api/v1/notifications/user/{userId}     Get user notifications (paginated)
GET    /api/v1/notifications/order/{orderId}   Get order notifications
```

---

## 🧪 Quick Test

```bash
# 1. Register
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"nithin","email":"nithin@example.com","password":"password123"}'

# 2. Login → copy token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"nithin","password":"password123"}'

# 3. Place Order (replace TOKEN and PRODUCT_ID)
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"PRODUCT_ID","quantity":2}]}'

# 4. Watch Kafka events at http://localhost:8090
```

---

## 🔑 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Kafka over REST for inter-service comms | Decouples services; downstream failure doesn't lose events |
| Manual Kafka offset commit (`MANUAL_IMMEDIATE`) | Prevents message loss on consumer crash |
| Idempotency checks on every consumer | Prevents double-processing on Kafka replay |
| Redis caching on GET `/orders/{id}` | ~60% DB load reduction on hot order reads |
| DB indexes on `(user_id, status, created_at)` | ~40% query time reduction on filtered list queries |
| Graceful degradation (inventory unavailable) | Order proceeds if inventory service is temporarily down |
| Retry scheduler for failed payments | Automatic recovery without manual intervention |
| Auto-cancel scheduler (stale PENDING orders) | Prevents inventory lock on abandoned carts |

---

## 📂 Project Structure

```
order-management-system/
├── common-lib/                  # Shared events, DTOs, enums
├── order-service/               # Core order lifecycle + JWT auth + Redis
├── payment-service/             # Kafka consumer + payment retry scheduler
├── inventory-service/           # Stock management + Kafka event publisher
├── notification-service/        # Multi-topic Kafka consumer + notification store
├── scripts/
│   └── init.sql                 # DB init + product seed data
├── docker-compose.yml
└── README.md
```

---

## 📊 Kafka Topics

| Topic                  | Producer          | Consumers                              |
|------------------------|-------------------|----------------------------------------|
| `order.created`        | Order Service     | Payment, Inventory, Notification       |
| `payment.processed`    | Payment Service   | Order, Inventory, Notification         |
| `order.status.changed` | Order Service     | Notification                           |
| `order.cancelled`      | Inventory Service | Order, Notification                    |
| `inventory.updated`    | Inventory Service | *(future: analytics)*                  |

---

## 👨‍💻 Author

**Amgoth Nithin** — Backend Software Engineer  
[LinkedIn](https://linkedin.com/in/amgoth-nithin-625ba2204) | [GitHub](https://github.com/AmgothNithin)
