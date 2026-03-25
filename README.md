# CineBook — Movie Ticket Booking Platform

> **Interview Project** | Senior Java/Spring Boot Microservices — B2B + B2C Platform

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Microservices](#4-microservices)
5. [Frontend](#5-frontend)
6. [Design Patterns](#6-design-patterns)
7. [Database Schema](#7-database-schema)
8. [API Contracts](#8-api-contracts)
9. [Kafka Event Flow](#9-kafka-event-flow)
10. [Running the Project](#10-running-the-project)
11. [Non-Functional Requirements](#11-non-functional-requirements)
12. [Project Structure](#12-project-structure)

---

## 1. Project Overview

**Problem Statement:** XYZ wants to build an online movie ticket booking platform that caters to both B2B (theatre partners) and B2C (end customers).

### Key Goals

- **B2B** — Enable theatre partners to onboard their theatres, manage screens, allocate seats, and create/update/delete shows
- **B2C** — Enable end customers to browse movies across cities, languages, and genres, and book tickets in advance with a seamless experience

### Functional Scenarios Implemented

**Read Scenarios:**
- Browse theatres currently running a selected movie in a city, including show timings by chosen date
- Booking platform offers: 50% discount on 3rd ticket, 20% discount on afternoon shows

**Write Scenarios:**
- Book movie tickets by selecting a theatre, timing, and preferred seats
- Theatres can create, update, and delete shows for the day
- Bulk booking and cancellation
- Theatres can allocate seat inventory and update them for the show

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Clients                                                      │
│  B2C React App (:3000)  │  B2B Thymeleaf (:8085/admin)      │
└────────────────┬─────────────────────────┬───────────────────┘
                 │                         │
                 ▼                         ▼
┌─────────────────────────────────────────────────────────────┐
│  API Gateway — Spring Cloud Gateway (:8080)                   │
│  JWT Auth Filter · Rate Limiting · Routing · Circuit Breaker  │
└─────────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  Service Discovery — Eureka Server (:8761)                    │
└─────────────────────────────────────────────────────────────┘
                 │
    ┌────────────┼────────────────────────────┐
    ▼            ▼            ▼               ▼
user-svc    movie-svc   theatre-svc     booking-svc
  :8081       :8082        :8085           :8083
                                              │
                               ┌──────────────┼──────────────┐
                               ▼              ▼              ▼
                          payment-svc    offer-svc     notification
                            :8084          :8086        (Kafka)
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
               Apache Kafka           Redis
             (async events)       (seat locking)
```

### Saga Choreography Pattern — Booking Flow

```
Customer → POST /bookings
    │
    ├─ Redis SETNX seat lock (10-min TTL)
    ├─ Persist Booking (PENDING_PAYMENT)
    └─ Publish BOOKING_INITIATED → Kafka
                │
                ▼
         payment-service
    ├─ Stripe PaymentIntent created
    └─ Stripe Webhook fires
                │
         ┌──────┴──────┐
         ▼             ▼
   PAYMENT_SUCCESS  PAYMENT_FAILED
         │             │
         ▼             ▼ (compensating transaction)
   Booking=CONFIRMED  Release Redis locks
   Publish BOOKING_CONFIRMED  Booking=FAILED
         │
         ▼
   notification-service → Email/SMS
```

---

## 3. Technology Stack

| Category | Technology | Purpose |
|---|---|---|
| Language | Java 17 | Core language (records, sealed classes) |
| Framework | Spring Boot 3.2.3 | Microservice foundation |
| API Gateway | Spring Cloud Gateway | Single entry point, JWT, rate limiting |
| Service Discovery | Netflix Eureka | Dynamic service registration + lb:// routing |
| Messaging | Apache Kafka | Booking saga events, async notifications |
| Cache / Lock | Redis | Seat locking (SETNX), session caching |
| ORM | Spring Data JPA + Hibernate | Entity persistence |
| DB (Dev) | H2 In-Memory | Zero-setup local development |
| DB (Prod) | PostgreSQL | Production persistence |
| DB Migration | Flyway | Schema versioning |
| Security | Spring Security + JWT (jjwt 0.12.3) | Auth + RBAC |
| Circuit Breaker | Resilience4j | Downstream fault tolerance |
| Documentation | SpringDoc OpenAPI 3 | Auto-generated Swagger UI |
| B2C Frontend | React 18 + Vite | Customer-facing SPA |
| B2B Frontend | Thymeleaf + Spring MVC | Theatre admin SSR portal |
| HTTP Client | Axios | React API calls |
| Build Tool | Maven (multi-module) | All 8 modules in one parent POM |
| IDE | IntelliJ IDEA | Development + demo |
| Containerisation | Docker + Docker Compose | Local infra (Kafka, Redis, Postgres) |
| AI | Spring AI + Gemini API | Movie recommendations |
| Monitoring | Micrometer + Prometheus + Grafana | Metrics |
| Logging | ELK Stack (Elasticsearch, Logstash, Kibana) | Log aggregation |
| Tracing | Spring Cloud Sleuth + Zipkin | Distributed tracing |

---

## 4. Microservices

### Service Registry

| Service | Port | Description |
|---|---|---|
| `eureka-server` | 8761 | Netflix Eureka service registry |

### API Gateway

| Service | Port | Description |
|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway — JWT auth, routing, circuit breaker |

### Business Services

| Service | Port | Responsibilities |
|---|---|---|
| `user-service` | 8081 | Registration, login, JWT generation, RBAC roles |
| `movie-service` | 8082 | Movie catalogue, show listings, browse by city/language/genre |
| `theatre-service` | 8085 | B2B onboarding, screens, seat allocation, show CRUD + Thymeleaf admin |
| `booking-service` | 8083 | Booking saga, Redis seat lock, bulk booking, cancellation |
| `payment-service` | 8084 | Stripe adapter, webhook handler, refund processing |
| `offer-service` | 8086 | Discount Strategy pattern — THIRD50, AFTERNOON20 |

### User Roles

| Role | Access |
|---|---|
| `CUSTOMER` | Browse movies, book tickets, view/cancel own bookings |
| `THEATRE_ADMIN` | Manage theatre, screens, seats, shows via admin portal |
| `SUPER_ADMIN` | Platform-wide administration |

---

## 5. Frontend

### B2C — React.js Customer Portal

**URL:** `http://localhost:3000`

| Page | URL | API Called |
|---|---|---|
| Login | `/login` | `POST /api/v1/auth/login` |
| Register | `/register` | `POST /api/v1/auth/register` |
| Browse Movies | `/movies` | `GET /api/v1/movies?city=&language=&genre=` |
| Show Listings | `/movies/:id/shows` | `GET /api/v1/movies/{id}/shows?city=&date=` |
| Seat Selection + Booking | `/booking/:showId` | `POST /api/v1/bookings` |
| My Bookings | `/my-bookings` | `GET /api/v1/bookings/my` |
| Offers | `/offers` | `GET /api/v1/offers` |
| Admin Dashboard | `/admin/dashboard` | `GET /api/v1/theatres/{id}/shows` |
| Admin Shows | `/admin/shows` | `POST/DELETE /api/v1/theatres/{id}/shows` |

**Setup:**
```bash
cd movie-frontend
npm install
npm run dev
# Opens at http://localhost:3000
# All /api calls proxy to http://localhost:8080 (API Gateway)
```

**Key features:**
- JWT stored in localStorage, attached to every Axios request
- Role-based routing — CUSTOMER sees booking pages, THEATRE_ADMIN sees admin pages
- Visual seat map with REGULAR / PREMIUM / RECLINER categories and pricing
- Live offer code application (THIRD50, AFTERNOON20)
- Mock data fallback when backend is not running — UI works standalone

### B2B — Thymeleaf Theatre Admin Portal

**URL:** `http://localhost:8085/admin`

| Page | URL | Description |
|---|---|---|
| Login | `/admin/login` | Session-based form login (Spring Security) |
| Dashboard | `/admin/dashboard` | Stats: total shows, active shows, seat inventory |
| Shows List | `/admin/shows` | All shows with Edit / Cancel actions |
| Create Show | `/admin/shows/new` | Form to create a new show |
| Edit Show | `/admin/shows/{id}/edit` | Update show time, type, price multiplier |
| Screens & Seats | `/admin/screens` | Add screens, view seat layout grid |
| Theatre Profile | `/admin/theatre` | Registered theatre details |

**Demo credentials:**
```
Email:    admin@cinebook.com
Password: Admin@123

Email:    manager@pvr.com
Password: Manager@123
```

**Why Thymeleaf for B2B:**
Server-side rendering is simpler to secure for internal tools, works without JavaScript, easier to maintain, and CSRF protection is built-in via Spring Security.

---

## 6. Design Patterns

| Pattern | Where Implemented | Class / File |
|---|---|---|
| **API Gateway** | Single entry point, JWT filter, rate limiting | `JwtAuthFilter.java`, `application.yml` routes |
| **Service Discovery** | Eureka + `lb://` URI resolution | `EurekaServerApplication.java`, all services |
| **Saga (Choreography)** | Booking → Payment → Notification via Kafka | `BookingService.java` |
| **Strategy** | Discount rules — each offer is a separate class | `DiscountStrategy.java`, `ThirdTicketDiscountStrategy.java`, `AfternoonShowDiscountStrategy.java` |
| **Factory** | Resolves correct discount strategy at runtime | `DiscountStrategyFactory.java` |
| **Adapter** | Stripe gateway wrapped behind interface | `StripeGatewayAdapter.java implements PaymentGateway` |
| **Circuit Breaker** | Resilience4j per downstream service call | `api-gateway/application.yml` |
| **Builder** | All entity and DTO construction | `Booking.builder()...build()` |
| **Observer** | Kafka producer/consumer for booking events | `BookingEvents.java` |
| **Null Object** | Default strategy when offer code unknown | `NoDiscountStrategy.java` |
| **Idempotency** | Prevent duplicate bookings and payments | `idempotencyKey` on Booking + Payment |
| **Optimistic Locking** | Prevent seat count conflicts | `@Version` on `TheatreShow` |
| **CQRS** | Separate read (show listings) from write (booking) | `MovieRepository` JPQL queries |

---

## 7. Database Schema

Each service owns its own schema (database-per-service pattern):

```
users              → user-service
movies, shows      → movie-service
theatre_partners,
screens,
seat_layout,
theatre_shows      → theatre-service
bookings,
booking_seats      → booking-service
payments           → payment-service
offers             → offer-service
```

### Key tables

```sql
-- Users (CUSTOMER | THEATRE_ADMIN | SUPER_ADMIN)
users(id UUID PK, name, email UNIQUE, password_hash, role, city, is_active)

-- Movies and Shows
movies(id UUID PK, title, language, genre, duration_mins, rating, release_date)
shows(id UUID PK, movie_id FK, screen_id, theatre_id, show_time, show_type, price_multiplier, available_seats)

-- Booking (central aggregate for saga)
bookings(id UUID PK, user_id FK, show_id FK, status, total_amount, discount_applied, offer_code, idempotency_key, expires_at)
booking_seats(id UUID PK, booking_id FK, seat_layout_id FK, seat_label, seat_price, status)

-- Payment
payments(id UUID PK, booking_id FK UNIQUE, gateway, transaction_id, amount, status, idempotency_key, paid_at)

-- Theatre
theatre_partners(id UUID PK, user_id FK, theatre_name, city, state, gst_number, status)
screens(id UUID PK, theatre_id FK, screen_name, total_seats, screen_type)
seat_layout(id UUID PK, screen_id FK, row_label, seat_number, seat_category, base_price)
```

---

## 8. API Contracts

**Base URL:** `http://localhost:8080/api/v1` (via API Gateway)

### Auth (public)
```
POST /auth/register    → { name, email, password, role, city }  → { token, userId, role }
POST /auth/login       → { email, password }                     → { token, userId, role }
GET  /users/me         → JWT required → user profile
```

### Movies (public)
```
GET  /movies                              → ?city=&language=&genre=&page=&size=
GET  /movies/{movieId}/shows              → ?city=Bengaluru&date=2025-03-25
GET  /movies/{movieId}                    → movie detail
POST /movies                              → Admin: add movie { title, language, genre, ... }
```

### Booking (JWT required)
```
POST   /bookings           → { showId, seatLayoutIds[], offerCode }
POST   /bookings/bulk      → { showId, bookings: [{ seatLayoutIds, userId }] }
GET    /bookings/my        → ?status=CONFIRMED&page=0&size=10
GET    /bookings/{id}      → booking detail with seats
DELETE /bookings/{id}      → cancel + initiate refund
```

### Theatre (THEATRE_ADMIN role)
```
POST   /theatres                                    → onboard theatre
GET    /theatres?city=Bengaluru                     → list theatres by city
POST   /theatres/{id}/shows                         → create show
PUT    /theatres/{id}/shows/{showId}                → update show
DELETE /theatres/{id}/shows/{showId}                → cancel show (soft delete)
POST   /theatres/{id}/screens                       → add screen
POST   /theatres/{id}/screens/{screenId}/seats      → allocate seat inventory
```

### Offers (public)
```
GET  /offers             → list active offers
POST /offers/apply       → ?offerCode=THIRD50&ticketCount=3&basePrice=250&showTime=
```

### Payment
```
POST /payments/initiate                        → { bookingId, amount, currency, gateway }
POST /payments/webhook                         → Stripe webhook (Stripe-Signature header)
GET  /payments/booking/{bookingId}             → payment status
POST /payments/demo/simulate-success/{id}      → DEMO: simulate payment (testing only)
```

### Swagger UI (per service)
```
http://localhost:8081/swagger-ui.html   → user-service
http://localhost:8082/swagger-ui.html   → movie-service
http://localhost:8083/swagger-ui.html   → booking-service
http://localhost:8084/swagger-ui.html   → payment-service
http://localhost:8085/swagger-ui.html   → theatre-service
http://localhost:8086/swagger-ui.html   → offer-service
```

---

## 9. Kafka Event Flow

| Topic | Producer | Consumer | Events |
|---|---|---|---|
| `booking-events` | booking-service | payment-service | `BOOKING_INITIATED`, `BOOKING_CANCELLED` |
| `payment-events` | payment-service | booking-service | `PAYMENT_SUCCESS`, `PAYMENT_FAILED` |
| `notification-events` | booking-service | notification-service | `BOOKING_CONFIRMED`, `BOOKING_CANCELLED` |

---

## 10. Running the Project

### Option A — Dev Mode (No Docker — H2 in-memory)

**Prerequisites:** Java 17, Maven 3.8+, Node.js 18+

**Step 1 — Build backend**
```bash
cd movie-ticket-platform
mvn clean install -DskipTests
```

**Step 2 — Start services IN ORDER**
```
1. EurekaServerApplication      → http://localhost:8761
2. ApiGatewayApplication        → http://localhost:8080
3. UserServiceApplication       → http://localhost:8081
4. MovieServiceApplication      → http://localhost:8082
5. BookingServiceApplication    → http://localhost:8083
6. PaymentServiceApplication    → http://localhost:8084
7. TheatreServiceApplication    → http://localhost:8085
8. OfferServiceApplication      → http://localhost:8086
```

In IntelliJ: `View → Tool Windows → Services` to manage all services from one panel.

**Step 3 — Start React frontend**
```bash
cd movie-frontend
npm install
npm run dev
# → http://localhost:3000
```

**Step 4 — Access B2B admin portal**
```
http://localhost:8085/admin
Email:    admin@cinebook.com
Password: Admin@123
```

### Option B — Full Stack with Docker (Kafka + Redis + Postgres)

```bash
cd movie-ticket-platform

# Start infrastructure
docker-compose up postgres redis zookeeper kafka -d

# Start all services
mvn spring-boot:run -pl eureka-server &
mvn spring-boot:run -pl api-gateway &
# ... repeat for each service

# Or build jars and run
mvn clean package -DskipTests
java -jar eureka-server/target/eureka-server.jar &
java -jar api-gateway/target/api-gateway.jar &
# ... etc
```

### Demo Booking Flow (curl)

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Muralidhar","email":"m@test.com","password":"Test@1234","role":"CUSTOMER","city":"Bengaluru"}'

# 2. Login → copy token from response
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"m@test.com","password":"Test@1234"}'

# 3. Browse movies
curl "http://localhost:8080/api/v1/movies?city=Bengaluru"

# 4. Book tickets (replace TOKEN and UUIDs)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"showId":"SHOW_UUID","seatLayoutIds":["S1","S2","S3"],"offerCode":"THIRD50"}'

# 5. Simulate payment success (demo mode)
curl -X POST http://localhost:8080/api/v1/payments/demo/simulate-success/BOOKING_UUID

# 6. View confirmed booking
curl http://localhost:8080/api/v1/bookings/BOOKING_UUID \
  -H "Authorization: Bearer TOKEN"
```

### H2 Console (Dev Mode)

| Service | URL | JDBC URL |
|---|---|---|
| user-service | http://localhost:8081/h2-console | `jdbc:h2:mem:userdb` |
| movie-service | http://localhost:8082/h2-console | `jdbc:h2:mem:moviedb` |
| booking-service | http://localhost:8083/h2-console | `jdbc:h2:mem:bookingdb` |
| payment-service | http://localhost:8084/h2-console | `jdbc:h2:mem:paymentdb` |
| theatre-service | http://localhost:8085/h2-console | `jdbc:h2:mem:theatredb` |
| offer-service | http://localhost:8086/h2-console | `jdbc:h2:mem:offerdb` |

Username: `sa` | Password: *(blank)*

---

## 11. Non-Functional Requirements

### Scalability — 99.99% Availability

- **Stateless microservices** — JWT auth, any pod handles any request
- **Kubernetes HPA** — auto-scale booking-service independently during peak
- **Redis seat lock TTL** — self-healing, no manual cleanup needed
- **CQRS** — read replicas serve browse queries, primary DB handles writes only
- **CDN** — CloudFront for movie posters and static assets

### Security — OWASP Top 10

| Threat | Mitigation |
|---|---|
| Broken Access Control | Spring Security RBAC, `@PreAuthorize`, users scoped to own data |
| Injection | Spring Data JPA parameterized queries, Bean Validation |
| Auth Failures | JWT short expiry (1h), BCrypt passwords, account lockout |
| Cryptographic | TLS 1.3, RS256 JWT signing, secrets in AWS Secrets Manager |
| Security Misconfiguration | Actuator endpoints secured, H2 console disabled in prod |

### Compliance

- **PCI-DSS** — Card data never stored; Stripe handles PCI scope
- **DPDP Act 2023** — User data deletion API available
- **RBI Guidelines** — Payment audit logs retained 7 years
- **GST** — GST number captured during theatre onboarding

### Monitoring

```
Metrics:  Micrometer → Prometheus → Grafana dashboards
Logging:  ELK Stack (Elasticsearch + Logstash + Kibana)
Tracing:  Spring Cloud Sleuth + Zipkin (correlation ID per request)
Health:   /actuator/health on every service
```

---

## 12. Project Structure

```
movie-ticket-platform/          ← Maven parent POM (8 modules)
├── pom.xml                     ← Parent: Spring Boot 3.2.3, Spring Cloud 2023.0.1
├── docker-compose.yml          ← Postgres + Redis + Kafka + Zookeeper
│
├── eureka-server/              ← @EnableEurekaServer — port 8761
├── api-gateway/                ← Spring Cloud Gateway — port 8080
│   └── filter/JwtAuthFilter    ← validates JWT, injects X-User-Id header
│   └── config/RateLimiterConfig← ipKeyResolver, userKeyResolver beans
│   └── fallback/FallbackController ← circuit breaker responses
│
├── user-service/               ← port 8081
│   └── entity/User.java        ← CUSTOMER | THEATRE_ADMIN | SUPER_ADMIN
│   └── security/JwtUtil.java   ← HS256 token generation
│   └── service/UserService.java← register, login, BCrypt
│
├── movie-service/              ← port 8082
│   └── entity/Movie.java + Show.java
│   └── repository/MovieRepository ← browseMovies JPQL (READ scenario)
│   └── repository/ShowRepository  ← findShowsForMovieInCityOnDate (READ scenario)
│
├── theatre-service/            ← port 8085
│   ├── REST API (TheatreController)   ← /api/v1/theatres/**
│   ├── web/AdminWebController         ← Thymeleaf /admin/**
│   ├── config/AdminSecurityConfig     ← session auth for admin portal
│   └── resources/templates/admin/    ← login, dashboard, shows, screens, theatre
│   └── resources/static/css/admin.css
│
├── booking-service/            ← port 8083
│   └── service/SeatLockService ← Redis SETNX + 10-min TTL
│   └── service/BookingService  ← full Saga: lock→persist→Kafka→confirm/compensate
│   └── event/BookingEvents.java← all Kafka event DTOs
│
├── payment-service/            ← port 8084
│   └── gateway/StripeGatewayAdapter ← Adapter pattern
│   └── service/PaymentService  ← webhook handler, Kafka saga participant
│
└── offer-service/              ← port 8086
    └── strategy/DiscountStrategy        ← Strategy interface
    └── strategy/ThirdTicketDiscount     ← 50% off 3rd ticket
    └── strategy/AfternoonShowDiscount   ← 20% off afternoon shows
    └── strategy/DiscountStrategyFactory ← Factory + Open/Closed Principle

movie-frontend/                 ← React 18 + Vite (separate folder)
├── vite.config.js              ← proxy /api → localhost:8080
├── src/
│   ├── context/AuthContext.jsx ← JWT management, role-based access
│   ├── services/api.js         ← Axios with JWT interceptor
│   ├── components/common/Navbar.jsx
│   └── pages/
│       ├── LoginPage.jsx
│       ├── RegisterPage.jsx
│       ├── MoviesPage.jsx      ← browse with city/language/genre filters
│       ├── ShowsPage.jsx       ← shows by movie + city + date (READ scenario)
│       ├── BookingPage.jsx     ← visual seat map + offer codes
│       ├── MyBookingsPage.jsx  ← booking history + cancellation
│       ├── OffersPage.jsx
│       └── admin/
│           ├── AdminDashboard.jsx
│           └── AdminShows.jsx
```

---

*Built for Calpion Software Technologies interview — Senior Backend Java/Spring Boot Microservices role*
*Author: Muralidhar Nayani | Bengaluru, Karnataka*
