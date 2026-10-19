# Car Rental Service – Low Level Design (LLD)

## 📋 Table of Contents
1. [Functional Requirements](#1-functional-requirements)
2. [Non-Functional Requirements](#2-non-functional-requirements)
3. [Class Diagram](#3-class-diagram)
4. [Design Patterns](#4-design-patterns)
5. [Database Architecture](#5-database-architecture)
6. [API Reference](#6-api-reference)
7. [Project Structure](#7-project-structure)
8. [Running Locally](#8-running-locally)

---

## 1. Functional Requirements

### Core User Features
| # | Requirement |
|---|---|
| FR-01 | User can **register** with name, email, password, phone, and driving licence number |
| FR-02 | User can **login** with email/password and receive a JWT token |
| FR-03 | User can **browse cars** filtered by city, category (Economy/SUV/Luxury), fuel type, and availability dates |
| FR-04 | User can **search cars** by make/model/features using full-text (Elasticsearch) |
| FR-05 | User can **view car details** including branch location, pricing, and availability calendar |
| FR-06 | User can **book a car** for a date range (system locks the car against concurrent bookings) |
| FR-07 | User can **pay** via Credit Card, UPI, or Wallet (Strategy pattern – swappable at runtime) |
| FR-08 | User receives **booking confirmation** via email + SMS after successful payment |
| FR-09 | User can **cancel a booking** and receive a refund minus a cancellation penalty |
| FR-10 | User can **return a car** – system calculates overstay charges and damage fees |
| FR-11 | User can **view rental history** with payment receipts |

### Admin Features
| # | Requirement |
|---|---|
| FR-12 | Admin can **add/update/deactivate cars** in the fleet |
| FR-13 | Admin can **manage branches** (add, update geo-coordinates, operating hours) |
| FR-14 | Admin can **override rental status** (e.g., force-complete a stuck rental) |
| FR-15 | Admin can **view fleet reports** (utilisation rate, revenue per car/branch) |

---

## 2. Non-Functional Requirements

| Category | Requirement | Approach |
|---|---|---|
| **Availability** | 99.99% uptime | Multi-AZ PostgreSQL, Redis Sentinel, Kafka replication factor 3 |
| **Scalability** | 5 000 concurrent bookings | Stateless Spring Boot pods + Kubernetes HPA; Redis distributed locking |
| **Performance** | Car search < 50 ms | Elasticsearch inverted index + geo query |
| **Performance** | Availability check < 10 ms | Redis SETNX atomic flag per carId |
| **Consistency** | No double-booking | `@Version` (optimistic lock) on `Car` entity + Redis distributed lock |
| **Durability** | No lost bookings | PostgreSQL ACID + Kafka event replay from offset |
| **Security** | Protect PII and payments | JWT auth, BCrypt passwords, HTTPS, `@PreAuthorize` |
| **Idempotency** | Re-submitted bookings don't double-charge | `rentalReference` as idempotency key on Payment |
| **Observability** | End-to-end tracing | Spring Actuator + structured JSON logging + Kafka event audit trail |
| **Extensibility** | Add new payment gateways | Strategy + Factory pattern – zero changes to existing code |

---

## 3. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        ENTITY RELATIONSHIPS                           │
└──────────────────────────────────────────────────────────────────────┘

┌──────────┐        ┌──────────┐        ┌──────────┐
│   User   │1      N│  Rental  │N      1│   Car    │
│──────────│────────│──────────│────────│──────────│
│id        │        │id        │        │id        │
│name      │        │user(FK)  │        │make      │
│email     │        │car(FK)   │        │model     │
│password  │        │pickupBr  │        │year      │
│phone     │        │dropoffBr │        │plate     │
│licence   │        │startDate │        │status    │
│role      │        │endDate   │        │dailyRate │
└──────────┘        │status    │        │fuelType  │
                    │totalAmt  │        │category  │
                    │rentalRef │        │@Version ◄┼── Optimistic Lock
                    └─────┬────┘        └──────────┘
                          │1
                         1│
                    ┌─────┴────┐
                    │ Payment  │
                    │──────────│
                    │id        │
                    │rental    │
                    │amount    │
                    │method    │
                    │status    │
                    │txnId     │
                    └──────────┘

┌──────────────┐       ┌──────────┐       ┌──────────────────┐
│  CarCategory │1────N│   Car    │N──────1│     Branch       │
│──────────────│       └──────────┘        │──────────────────│
│id            │  (via CarBranchAssignment)│id               │
│type (enum)   │                           │name             │
│description   │                           │city             │
│baseDailyRate │                           │address          │
└──────────────┘                           │lat, lon         │
                                           └──────────────────┘

┌──────────────────────── STRATEGY PATTERN ─────────────────────────┐
│                                                                    │
│  <<interface>>              concrete implementations               │
│  PaymentStrategy  ◄──────── CreditCardPaymentStrategy             │
│  + process()      ◄──────── UpiPaymentStrategy                    │
│  + refund()       ◄──────── WalletPaymentStrategy                 │
│  + getMethod()                                                     │
│                   ▲                                                │
│  PaymentProcessorFactory ─ resolves by PaymentMethod enum (O(1))  │
└────────────────────────────────────────────────────────────────────┘

┌──────────────────────── OBSERVER PATTERN ─────────────────────────┐
│                                                                    │
│  RentalEventPublisher (Kafka Producer / Subject)                   │
│    + publishRentalBooked(rental)                                   │
│    + publishRentalCancelled(rental)                                │
│    + publishRentalReturned(rental)                                 │
│                                                                    │
│  <<interface>> RentalEventObserver                                 │
│    + onRentalBooked(event)                                         │
│    + onRentalCancelled(event)                                      │
│         ▲                ▲                ▲                        │
│   EmailObserver    SMSObserver    AnalyticsObserver                │
└────────────────────────────────────────────────────────────────────┘

┌──────────────────────── FACADE PATTERN ───────────────────────────┐
│                                                                    │
│  RentalController ──► RentalFacade (Facade)                       │
│                              │                                     │
│                              ├── CarAvailabilityService (Redis)    │
│                              ├── RentalService (JPA)               │
│                              ├── PaymentService (Strategy)         │
│                              └── RentalEventPublisher (Kafka)      │
└────────────────────────────────────────────────────────────────────┘
```

---

## 4. Design Patterns

### 4.1 Strategy Pattern – Payment Processing
**Location:** `com.carrental.strategy`
```
PaymentService → PaymentProcessorFactory.getProcessor(CREDIT_CARD)
                                          ↓
                            CreditCardPaymentStrategy.process()

Swap to UPI: PaymentProcessorFactory.getProcessor(UPI)
                                          ↓
                            UpiPaymentStrategy.process()
```
Adding a new gateway = one new `@Component` class. Zero changes to Factory or Service.

---

### 4.2 Factory Pattern – Payment Processor Resolution
**Location:** `com.carrental.factory.PaymentProcessorFactory`
```java
// Factory builds a map at startup:
Map<CREDIT_CARD, CreditCardPaymentStrategy>
Map<UPI,         UpiPaymentStrategy>
Map<WALLET,      WalletPaymentStrategy>

// O(1) lookup:
strategy = factory.getProcessor(PaymentMethod.UPI);
```

---

### 4.3 Observer Pattern – Rental Event Notifications
**Location:** `com.carrental.observer` + `com.carrental.kafka`
```
RentalFacade.book() → RentalEventPublisher.publishRentalBooked(event)
                              ↓ Kafka topic: rental.booked
                   EmailNotificationObserver.onRentalBooked()  → email
                   SMSNotificationObserver.onRentalBooked()    → SMS
                   AnalyticsObserver.onRentalBooked()          → metrics
```

---

### 4.4 Builder Pattern – Entity Construction
**Location:** All `@Entity` classes via Lombok `@Builder`
```java
// Without Builder:
new Rental(user, car, branch, branch, date, date, null, RentalStatus.PENDING, 0.0, ref)

// With Builder:
Rental.builder()
    .user(user).car(car).pickupBranch(branch)
    .startDate(start).endDate(end)
    .status(RentalStatus.PENDING)
    .rentalReference("CR-ABC123")
    .build();
```

---

### 4.5 Facade Pattern – Booking Orchestration
**Location:** `com.carrental.facade.RentalFacade`
```
Step 1: Acquire Redis distributed lock on carId
Step 2: Check car availability via CarAvailabilityService
Step 3: Validate dates and create PENDING Rental (RentalService)
Step 4: Process payment via PaymentService (Strategy dispatch)
Step 5a: [SUCCESS] Mark rental ACTIVE, update Car status, publish Kafka event
Step 5b: [FAILURE] Release Redis lock, cancel rental, propagate error
```

---

### 4.6 Repository Pattern
**Location:** `com.carrental.repository.jpa.*`  
Spring Data JPA interfaces abstract all SQL away from service layer.

---

### 4.7 Optimistic Locking – Concurrency Control
**Location:** `com.carrental.model.Car` – `@Version private Long version`
```java
// Thread 1 reads Car(version=0) → updates status to RENTED
// Thread 2 reads Car(version=0) → also tries to update
// Thread 1 succeeds → version becomes 1
// Thread 2 throws ObjectOptimisticLockingFailureException → 409 Conflict
```

---

## 5. Database Architecture

### PostgreSQL (Primary RDBMS)
- **ACID** transactions across Rental + Payment tables
- `@Version` optimistic locking on Car rows
- Foreign keys enforce referential integrity
- Flyway migrations version-control schema changes

### Redis (Caching + Distributed Lock)
- `car:lock:{carId}` – booking lock (TTL 30 s) using SETNX
- `car:availability:{carId}` – boolean flag (TTL 1 h) for O(1) checks
- `@Cacheable("cars")` – caches car detail responses to cut DB load

### Elasticsearch (Car Search)
- `CarDocument` index with analysed `make`, `model`, `features` fields
- Geo-point field `branchLocation` for branch-proximity search
- Fuzzy matching handles typos ("toyata" → "toyota")

### Apache Kafka (Async Events)
| Topic | Consumers |
|-------|-----------|
| `rental.booked` | NotificationService, AnalyticsService, InvoiceService |
| `rental.cancelled` | NotificationService, RefundService |
| `rental.returned` | NotificationService, BillingService |
| `payment.processed` | AuditService |

---

## 6. API Reference

### Auth
```
POST /api/auth/register   – Register new user → JWT
POST /api/auth/login      – Login → JWT
```

### Cars
```
GET  /api/cars                – Browse cars (filter: city, category, dates)
GET  /api/cars/{id}           – Car detail
GET  /api/cars/search?q=      – Full-text Elasticsearch search
POST /api/cars                – Add car (ADMIN)
PUT  /api/cars/{id}           – Update car (ADMIN)
DELETE /api/cars/{id}         – Deactivate car (ADMIN)
```

### Branches
```
GET /api/branches?city=       – Branches in a city
GET /api/branches/{id}        – Branch detail
```

### Rentals
```
POST /api/rentals/book             – Book a car (JWT required)
GET  /api/rentals/my               – My rental history (JWT required)
GET  /api/rentals/{id}             – Rental detail (JWT required)
PUT  /api/rentals/{id}/return      – Return car (JWT required)
POST /api/rentals/{id}/cancel      – Cancel booking (JWT required)
```

### Payments
```
GET /api/payments/{id}                – Payment detail
GET /api/payments/rental/{rentalId}   – Payment for a rental
```

### Admin
```
GET /api/admin/reports/revenue   – Revenue report
GET /api/admin/reports/fleet     – Fleet utilisation
```

---

## 7. Project Structure

```
com.carrental/
├── CarRentalApplication.java
├── config/
│   ├── SecurityConfig.java        JWT filter chain, BCrypt bean, CORS
│   ├── RedisConfig.java           RedisTemplate with JSON serialiser
│   └── KafkaConfig.java           Topic beans, producer/consumer factories
├── enums/
│   ├── UserRole.java              CUSTOMER / ADMIN
│   ├── CarStatus.java             AVAILABLE / RENTED / MAINTENANCE
│   ├── CarCategoryType.java       ECONOMY / COMPACT / SUV / LUXURY / VAN
│   ├── FuelType.java              PETROL / DIESEL / ELECTRIC / HYBRID
│   ├── RentalStatus.java          PENDING / ACTIVE / COMPLETED / CANCELLED
│   ├── PaymentMethod.java         CREDIT_CARD / UPI / WALLET
│   └── PaymentStatus.java         PENDING / SUCCESS / FAILED / REFUNDED
├── model/
│   ├── User.java                  JPA entity + Spring Security UserDetails
│   ├── CarCategory.java           Category master data
│   ├── Car.java                   @Version optimistic lock
│   ├── Branch.java                Pickup/drop-off location
│   ├── CarBranchAssignment.java   Car-to-branch mapping
│   ├── Rental.java                Booking lifecycle
│   ├── Payment.java               Payment record
│   ├── CancellationPolicy.java    Penalty rules per category
│   └── elastic/
│       └── CarDocument.java       Elasticsearch @Document
├── repository/
│   ├── jpa/                       Spring Data JPA repositories
│   └── elasticsearch/
│       └── CarSearchRepository.java
├── strategy/                      ← STRATEGY PATTERN
│   ├── PaymentStrategy.java       Interface
│   ├── CreditCardPaymentStrategy.java
│   ├── UpiPaymentStrategy.java
│   └── WalletPaymentStrategy.java
├── factory/                       ← FACTORY PATTERN
│   └── PaymentProcessorFactory.java
├── observer/                      ← OBSERVER PATTERN
│   ├── RentalEventObserver.java   Interface
│   ├── EmailNotificationObserver.java
│   ├── SMSNotificationObserver.java
│   └── AnalyticsObserver.java
├── kafka/
│   ├── RentalEventPublisher.java  Kafka producer (Subject)
│   ├── RentalEventConsumer.java   Kafka consumer (dispatches to observers)
│   └── events/
│       ├── RentalBookedEvent.java
│       └── RentalCancelledEvent.java
├── facade/                        ← FACADE PATTERN
│   └── RentalFacade.java
├── service/
│   ├── AuthService.java
│   ├── CarService.java
│   ├── BranchService.java
│   ├── RentalService.java
│   ├── ReturnService.java
│   ├── CancellationService.java
│   ├── PaymentService.java
│   ├── CarAvailabilityService.java
│   └── CarSearchService.java
├── controller/
│   ├── AuthController.java
│   ├── CarController.java
│   ├── BranchController.java
│   ├── RentalController.java
│   └── PaymentController.java
├── dto/
│   ├── request/                   Validated input DTOs
│   └── response/                  Output DTOs
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── CarNotAvailableException.java
│   ├── ResourceNotFoundException.java
│   ├── PaymentFailedException.java
│   └── InvalidRentalStateException.java
└── security/
    ├── JwtService.java
    ├── JwtAuthFilter.java
    └── UserDetailsServiceImpl.java
```

---

## 8. Running Locally

```bash
# Start infrastructure
docker-compose up -d postgres redis kafka elasticsearch

# Build and run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Default port: **8080**  
Actuator health: `GET /actuator/health`

