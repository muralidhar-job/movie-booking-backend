# Movie Ticket Booking Platform
## Senior Java/Spring Boot Interview Project

---

## Architecture
- **8 modules**: eureka-server, api-gateway, user-service, movie-service, theatre-service, booking-service, payment-service, offer-service
- **Design Patterns**: Saga (Choreography), Strategy, Factory, Adapter, Builder, Observer, Circuit Breaker, API Gateway, Service Discovery
- **Tech**: Java 17, Spring Boot 3.2, Spring Cloud Gateway, Eureka, Kafka, Redis, H2 (dev), PostgreSQL (prod)

---

## Quick Start in IntelliJ IDEA

### 1. Open project
File → Open → select `movie-ticket-platform/` folder → Trust Project

### 2. Install Maven dependencies
```
mvn clean install -DskipTests
```

### 3. Start infrastructure (Docker required)
```bash
docker-compose up postgres redis zookeeper kafka -d
```

### 4. Start services IN ORDER (Run each main class)
```
1. EurekaServerApplication     → http://localhost:8761
2. ApiGatewayApplication       → http://localhost:8080
3. UserServiceApplication      → http://localhost:8081
4. MovieServiceApplication     → http://localhost:8082
5. BookingServiceApplication   → http://localhost:8083
6. PaymentServiceApplication   → http://localhost:8084
7. TheatreServiceApplication   → http://localhost:8085
8. OfferServiceApplication     → http://localhost:8086
```

### 5. Dev profile (no Docker needed)
Each service has an H2 in-memory DB for `dev` profile.
Just run the main class directly — H2 console at `http://localhost:{port}/h2-console`

---

## API Endpoints (via Gateway on port 8080)

### Auth (public)
```
POST /api/v1/auth/register   → register customer or theatre admin
POST /api/v1/auth/login      → get JWT token
```

### Movies (public)
```
GET  /api/v1/movies                          → browse with ?city=&language=&genre=
GET  /api/v1/movies/{movieId}/shows?city=&date=  → READ SCENARIO: shows by city+date
```

### Booking (JWT required)
```
POST   /api/v1/bookings          → create booking (initiates Kafka saga)
POST   /api/v1/bookings/bulk     → bulk booking
GET    /api/v1/bookings/my       → my booking history
GET    /api/v1/bookings/{id}     → booking detail
DELETE /api/v1/bookings/{id}     → cancel + refund
```

### Theatre (THEATRE_ADMIN role)
```
POST /api/v1/theatres                              → onboard theatre
POST /api/v1/theatres/{id}/shows                   → create show
PUT  /api/v1/theatres/{id}/shows/{showId}          → update show
DEL  /api/v1/theatres/{id}/shows/{showId}          → delete show
POST /api/v1/theatres/{id}/screens/{screenId}/seats → allocate seats
```

### Offers (public)
```
GET  /api/v1/offers                  → list active offers
POST /api/v1/offers/apply            → validate + calculate discount
```

### Payment
```
POST /api/v1/payments/initiate                      → start Stripe payment
POST /api/v1/payments/webhook                       → Stripe webhook
POST /api/v1/payments/demo/simulate-success/{id}    → DEMO: simulate payment
```

---

## Demo Booking Flow (step by step)

```bash
# 1. Register a customer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Muralidhar","email":"m@test.com","password":"Test@1234","role":"CUSTOMER","city":"Bengaluru"}'

# 2. Login → copy the token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -d '{"email":"m@test.com","password":"Test@1234"}'

# 3. Browse movies in Bengaluru
curl "http://localhost:8080/api/v1/movies?city=Bengaluru"

# 4. Book tickets (replace token and IDs)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"showId":"<UUID>","seatLayoutIds":["<UUID1>","<UUID2>","<UUID3>"],"offerCode":"THIRD50"}'

# 5. Simulate payment success (demo mode)
curl -X POST http://localhost:8080/api/v1/payments/demo/simulate-success/<bookingId>

# 6. Check confirmed booking
curl http://localhost:8080/api/v1/bookings/<bookingId> \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Run Tests
```bash
# All tests
mvn test

# Specific service
mvn test -pl booking-service
mvn test -pl offer-service
```

---

## Design Patterns — Interview Reference

| Pattern | Where implemented |
|---|---|
| **API Gateway** | `api-gateway/JwtAuthFilter.java` + `application.yml` routes |
| **Service Discovery** | Eureka server + `@EnableDiscoveryClient` + `lb://` URIs |
| **Saga (Choreography)** | `BookingService` → Kafka → `PaymentService` → Kafka → `BookingService` |
| **Strategy** | `DiscountStrategy` interface + `ThirdTicketDiscountStrategy` + `AfternoonShowDiscountStrategy` |
| **Factory** | `DiscountStrategyFactory.resolve(offerCode)` |
| **Adapter** | `StripeGatewayAdapter implements PaymentGateway` |
| **Circuit Breaker** | `api-gateway/application.yml` → Resilience4j per route |
| **Builder** | All entity/DTO construction (`Booking.builder()...build()`) |
| **Observer** | Kafka producer/consumer — `BookingEvents`, `PaymentEvents` |
| **Null Object** | `NoDiscountStrategy` — never returns null from factory |
| **Idempotency** | `idempotencyKey` on Booking + Payment prevents duplicates |
| **Optimistic Locking** | `@Version` on `TheatreShow` — prevents seat count conflicts |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `booking-events` | booking-service | payment-service | BOOKING_INITIATED, BOOKING_CANCELLED |
| `payment-events` | payment-service | booking-service | PAYMENT_SUCCESS, PAYMENT_FAILED |
| `notification-events` | booking-service | notification-service | BOOKING_CONFIRMED, BOOKING_CANCELLED |

---

## Swagger UI (per service)
- User:    http://localhost:8081/swagger-ui.html
- Movie:   http://localhost:8082/swagger-ui.html
- Booking: http://localhost:8083/swagger-ui.html
- Payment: http://localhost:8084/swagger-ui.html
- Theatre: http://localhost:8085/swagger-ui.html
- Offer:   http://localhost:8086/swagger-ui.html

---

## Eureka Dashboard
http://localhost:8761 — shows all registered service instances
