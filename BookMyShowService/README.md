# BookMyShow – Low Level Design (LLD)

## 📋 Table of Contents
1. [Functional Requirements](#functional-requirements)
2. [Non-Functional Requirements](#non-functional-requirements)
3. [Class Diagram](#class-diagram)
4. [Design Patterns](#design-patterns)
5. [Database Architecture](#database-architecture)
6. [API Reference](#api-reference)
7. [Project Structure](#project-structure)

---

## 1. Functional Requirements

### Core User Features
| # | Requirement |
|---|---|
| FR-01 | User can **register** with name, email, password, phone number |
| FR-02 | User can **login** with email/password and receive a JWT token |
| FR-03 | User can **browse movies** by city, genre, language, rating |
| FR-04 | User can **search movies** by title (full-text), cast, genre |
| FR-05 | User can **view show timings** for a movie in their city |
| FR-06 | User can **view seat availability** map for a selected show |
| FR-07 | User can **select seats** (system holds them for 10 minutes) |
| FR-08 | User can **make payment** via Razorpay, Stripe, or Paytm |
| FR-09 | User receives **confirmation** (email + SMS) after successful booking |
| FR-10 | User can **view booking history** |
| FR-11 | User can **cancel a confirmed booking** and receive a refund |
| FR-12 | User can **view digital ticket** by booking reference |

### Admin Features
| # | Requirement |
|---|---|
| FR-13 | Admin can **add/update/deactivate movies** |
| FR-14 | Admin can **add theatres and screens** with seat layouts |
| FR-15 | Admin can **schedule shows** (movie + screen + time) |
| FR-16 | Admin can **view booking reports** and analytics |

---

## 2. Non-Functional Requirements

| Category | Requirement | Approach |
|---|---|---|
| **Availability** | 99.99% uptime | Multi-AZ deployment on AWS/GCP |
| **Scalability** | Handle 10K concurrent users during flash sales | Stateless Spring Boot + Kubernetes HPA |
| **Performance** | Seat availability reads < 50ms | Redis caching (O(1) lookup) |
| **Performance** | Movie search < 100ms | Elasticsearch inverted index |
| **Consistency** | No double-booking | Redis SETNX atomic lock + PostgreSQL @Version optimistic lock |
| **Durability** | No lost bookings | PostgreSQL ACID transactions + Kafka event replay |
| **Security** | Protect user data and payments | JWT auth, BCrypt passwords, HTTPS, @PreAuthorize |
| **Idempotency** | Re-submitted payments don't double-charge | Booking reference as idempotency key |
| **Observability** | Monitor all booking flows | Spring Actuator + structured logging + Kafka events |
| **Extensibility** | Add new payment gateways easily | Strategy + Factory pattern |

---

## 3. Class Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ENTITY RELATIONSHIPS                          │
└─────────────────────────────────────────────────────────────────────┘

┌──────────┐         ┌──────────┐         ┌──────────┐
│   User   │1      N │ Booking  │N       1│   Show   │
│──────────│─────────│──────────│─────────│──────────│
│id        │         │id        │         │id        │
│name      │         │user (FK) │         │movie(FK) │
│email     │         │show (FK) │         │screen(FK)│
│password  │         │status    │         │startTime │
│phone     │         │totalAmt  │         │endTime   │
│role      │         │bookingRef│         │isActive  │
└──────────┘         └────┬─────┘         └────┬─────┘
                          │1                    │
                         N│                    1│
                    ┌─────┴────┐         ┌──────┴────┐
                    │Booking   │N       1│  Screen   │
                    │Item      │─────────│───────────│
                    │──────────│         │id         │
                    │booking   │         │name       │
                    │showSeat  │         │theatre(FK)│
                    │price     │         │totalSeats │
                    └──────────┘         └─────┬─────┘
                                               │1
                          ┌────────────────────┤
                          │                    │
                    ┌─────┴─────┐       ┌──────┴────┐
                    │ ShowSeat  │N     1│   Seat    │
                    │───────────│───────│───────────│
                    │id         │       │id         │
                    │show (FK)  │       │rowNumber  │
                    │seat (FK)  │       │seatNumber │
                    │status     │       │seatType   │
                    │version ◄──┼──── OPTIMISTIC    │
                    └───────────┘  LOCKING KEY      │
                                               ──────┘

┌──────────┐         ┌──────────┐         ┌──────────┐
│ Payment  │1       1│ Booking  │         │ Theatre  │1─────N│ Screen │
│──────────│─────────│          │         └──────────┘       └────────┘
│id        │         └──────────┘
│booking   │
│amount    │
│gateway   │
│status    │
│txnId     │
│refundId  │
└──────────┘

┌─────────────────── STRATEGY PATTERN ─────────────────────┐
│                                                           │
│  <<interface>>          concrete implementations          │
│  PaymentProcessor  ◄─── RazorpayProcessor                │
│  + charge()        ◄─── StripeProcessor                  │
│  + refund()        ◄─── PaytmProcessor                   │
│  + getGateway()                                          │
│                    ▲                                      │
│  PaymentProcessorFactory ────────────────────────────── │
│  (resolves strategy by PaymentGateway enum)              │
└───────────────────────────────────────────────────────────┘

┌──────────────────── OBSERVER PATTERN ─────────────────────┐
│                                                           │
│  BookingEventPublisher (Subject)                         │
│    observers: List<BookingEventObserver>                 │
│    + publishBookingConfirmed(booking)                    │
│    + publishBookingCancelled(booking)                    │
│                                                           │
│  <<interface>> BookingEventObserver                      │
│    + onBookingConfirmed(booking)                         │
│    + onBookingCancelled(booking)                         │
│         ▲              ▲              ▲                  │
│  EmailNotif  SMSNotif  AnalyticsObs  (extensible)        │
└───────────────────────────────────────────────────────────┘
```

---

## 4. Design Patterns

### 4.1 Strategy Pattern
**Location:** `com.bookmyshow.payment`
**Purpose:** Swap payment gateway implementations at runtime without changing client code.

```
PaymentService → PaymentProcessorFactory.getProcessor(RAZORPAY)
                                         ↓
                              RazorpayProcessor.charge()
                              
Swap to Stripe: PaymentProcessorFactory.getProcessor(STRIPE)
                                         ↓
                              StripeProcessor.charge()
```

**Marked in code:** `PaymentProcessor.java` (interface), `RazorpayProcessor.java`, `StripeProcessor.java`, `PaytmProcessor.java`

---

### 4.2 Factory Pattern
**Location:** `com.bookmyshow.payment.PaymentProcessorFactory`
**Purpose:** Resolve the correct PaymentProcessor strategy at runtime based on `PaymentGateway` enum.

```java
// Factory builds a map at startup:
Map<"RAZORPAY", RazorpayProcessor>
Map<"STRIPE",   StripeProcessor>
Map<"PAYTM",    PaytmProcessor>

// Lookup is O(1):
processor = factory.getProcessor(PaymentGateway.RAZORPAY);
```

Adding a new gateway = add `@Component` class + enum value. **Zero changes to factory or service.**

---

### 4.3 Observer Pattern
**Location:** `com.bookmyshow.observer`
**Purpose:** Decouple booking confirmation from email/SMS/analytics side effects.

```
BookingService.book() → eventPublisher.publishBookingConfirmed(booking)
                                              ↓ (notifies all)
                        EmailNotificationObserver.onBookingConfirmed()  → send email
                        SMSNotificationObserver.onBookingConfirmed()    → send SMS
                        AnalyticsObserver.onBookingConfirmed()          → record event
```

Observers run `@Async` – non-blocking. Booking response returned before emails send.

---

### 4.4 Builder Pattern
**Location:** Every `@Entity` class (Lombok `@Builder`)
**Purpose:** Construct complex objects fluently without telescoping constructors.

```java
// Without Builder (telescoping):
new Booking(user, show, null, BookingStatus.PENDING, 450.0, "BMS-ABC123", null, null)

// With Builder (readable, maintainable):
Booking.builder()
    .user(user)
    .show(show)
    .status(BookingStatus.PENDING)
    .totalAmount(450.0)
    .bookingReference("BMS-ABC123")
    .build();
```

---

### 4.5 Template Method Pattern
**Location:** `com.bookmyshow.service.BookingService.book()`
**Purpose:** Define a fixed algorithm skeleton (booking lifecycle steps) where individual steps can vary.

```
book() skeleton (fixed order):
  Step 1: Validate input
  Step 2: Lock seats in Redis (SeatLockService)
  Step 3: Create PENDING Booking
  Step 4: Process Payment (Strategy via PaymentService)
  Step 5a: [SUCCESS] Confirm → Notify Observers → Publish Kafka
  Step 5b: [FAILURE] Release locks → Cancel Booking
```

---

### 4.6 Repository Pattern
**Location:** `com.bookmyshow.repository` (all `*Repository` interfaces)
**Purpose:** Abstraction layer between business logic and data access. Services never write SQL/queries directly.

---

### 4.7 Facade Pattern
**Location:** `com.bookmyshow.service.BookingService`
**Purpose:** Single entry point for the complex booking workflow. Controllers don't need to know about SeatLockService, PaymentProcessorFactory, Kafka, or Observers.

```
BookingController                BookingService (Facade)
     │                                   │
     │  book(request)  ───────────────►  │ ── SeatLockService (Redis)
     │                                   │ ── PaymentService (Strategy)
     │◄─── BookingResponse ──────────── │ ── BookingEventPublisher (Observer)
                                        │ ── BookingEventProducer (Kafka)
```

---

### 4.8 Optimistic Locking (Concurrency Pattern)
**Location:** `com.bookmyshow.model.ShowSeat` (`@Version` field)
**Purpose:** Prevent double-booking under concurrent requests.

```java
@Version
private Long version;  // Auto-incremented by Hibernate on every UPDATE

// Thread 1 reads ShowSeat (version=0) → tries to UPDATE to BOOKED
// Thread 2 reads ShowSeat (version=0) → tries to UPDATE to BOOKED
// Thread 1 succeeds → version becomes 1
// Thread 2 fails: OptimisticLockingFailureException (version mismatch)
// → Thread 2 must retry
```

---

### 4.9 Proxy Pattern (Caching)
**Location:** `com.bookmyshow.service.MovieService` (`@Cacheable`, `@CacheEvict`)
**Purpose:** Transparently cache movie data in Redis. Callers don't know if data comes from cache or DB.

```java
@Cacheable(value = "movies", key = "#id")
public Movie getMovieById(Long id) { ... }
// First call: DB hit → result stored in Redis
// Subsequent calls: Redis hit → returned without DB query
```

---

## 5. Database Architecture

### PostgreSQL (Primary Store)
**Why PostgreSQL?**
- ACID transactions essential for booking + payment integrity
- Foreign key constraints prevent orphaned data
- `@Version` optimistic locking for concurrent seat updates
- Complex joins across Show → Screen → Theatre → City
- Time-series queries for show scheduling

**Key Tables:**
```sql
users         – authentication and profile data
movies        – content catalog (titles, genres, languages)
theatres      – cinema complexes with geo coordinates
screens       – auditoriums inside theatres
seats         – physical seat layout (row + number + type + price)
shows         – scheduled screenings (movie × screen × time)
show_seats    – per-show seat inventory with status + @Version
bookings      – user reservations with status lifecycle
booking_items – individual seat line items within a booking
payments      – payment transactions with gateway reference IDs
```

---

### Redis (Seat Locking + Caching)
**Why Redis?**
- **Atomic SETNX**: Set-if-Not-eXists prevents two users locking the same seat simultaneously
- **TTL**: Lock auto-expires after 10 minutes without any cron job
- **O(1)** read/write: seat availability checks under 1ms
- **@Cacheable**: Cache movie/show data to reduce PostgreSQL load

**Key Patterns:**
```
seat-lock:{showSeatId}    → userId (TTL: 10 min) — seat hold
movies::{id}              → Movie JSON           — movie cache
movies::all-active        → Movie[] JSON         — movie list cache
```

---

### Elasticsearch (Movie Search)
**Why Elasticsearch?**
- Full-text search with relevance scoring (inverted index)
- Fuzzy matching (handles typos: "avengrs" matches "Avengers")
- Aggregations for genre/language faceting
- Sub-10ms search at scale (vs. PostgreSQL `LIKE '%...%'` which is O(n))
- Schema-free: can add new searchable fields without migration

**Index:** `movies`
**Analyzed Fields:** `title`, `description`, `cast` (full-text)
**Keyword Fields:** `genre`, `language`, `certification` (exact filter)

---

### Apache Kafka (Async Events)
**Why Kafka?**
- Decouples booking confirmation from notifications (email/SMS/push)
- If notification service is down, events replay from offset on restart
- Multiple consumer groups process the same event independently
  (notifications, analytics, invoice generation, seat release)
- Guaranteed ordering per partition (bookingId as key)

**Topics:**
```
booking-confirmed   → consumed by: NotificationService, AnalyticsService, InvoiceService
booking-cancelled   → consumed by: NotificationService, RefundService, SeatReleaseService
```

---

## 6. API Reference

### Authentication
```
POST /api/auth/register    – Register new user → JWT
POST /api/auth/login       – Login → JWT
```

### Movies (Public)
```
GET  /api/movies           – List all active movies
GET  /api/movies/{id}      – Get movie by ID
POST /api/movies           – Create movie (ADMIN)
DELETE /api/movies/{id}    – Deactivate movie (ADMIN)
```

### Shows (Public)
```
GET  /api/shows?movieId={id}&city={city}  – Upcoming shows
GET  /api/shows/{showId}/seats             – Seat availability map
POST /api/shows                            – Create show (ADMIN)
```

### Bookings (JWT Required)
```
POST   /api/bookings           – Create booking (lock + pay + confirm)
GET    /api/bookings           – My booking history
GET    /api/bookings/{id}      – Get booking by ID
DELETE /api/bookings/{id}      – Cancel booking + refund
```

### Search (Public)
```
GET /api/search/movies?q={query}              – Full-text search
GET /api/search/movies/genre/{genre}          – Filter by genre
GET /api/search/movies/language/{language}    – Filter by language
GET /api/search/movies/top-rated?minRating=8  – Filter by rating
```

### Theatres (Public)
```
GET /api/theatres?city={city}    – Theatres in a city
GET /api/theatres/{id}            – Theatre details
```

---

## 7. Project Structure

```
com.bookmyshow/
├── BookMyShowApplication.java         Main entry point
├── config/
│   ├── SecurityConfig.java            JWT filter chain, BCrypt, CORS
│   ├── RedisConfig.java               Redis template with JSON serializer
│   └── SwaggerConfig.java             OpenAPI 3.0 documentation
├── enums/
│   ├── BookingStatus.java             PENDING/CONFIRMED/CANCELLED/REFUNDED
│   ├── SeatStatus.java                AVAILABLE/LOCKED/BOOKED
│   ├── SeatType.java                  NORMAL/PREMIUM/RECLINER
│   ├── PaymentStatus.java             INITIATED/SUCCESS/FAILED/REFUNDED
│   ├── PaymentGateway.java            RAZORPAY/STRIPE/PAYTM
│   ├── Genre.java                     ACTION/COMEDY/DRAMA/...
│   └── NotificationChannel.java       EMAIL/SMS/PUSH
├── model/
│   ├── User.java                      JPA entity + UserDetails
│   ├── Movie.java                     Movie catalog entity
│   ├── Theatre.java                   Cinema complex entity
│   ├── Screen.java                    Auditorium entity
│   ├── Seat.java                      Physical seat entity
│   ├── Show.java                      Scheduled screening entity
│   ├── ShowSeat.java                  Show-seat join with @Version
│   ├── Booking.java                   Booking lifecycle entity
│   ├── BookingItem.java               Per-seat line item
│   ├── Payment.java                   Payment transaction entity
│   ├── redis/
│   │   └── SeatLock.java              Redis TTL-based seat hold
│   └── elastic/
│       └── MovieDocument.java         Elasticsearch document
├── repository/
│   ├── UserRepository.java
│   ├── MovieRepository.java
│   ├── TheatreRepository.java
│   ├── ScreenRepository.java
│   ├── SeatRepository.java
│   ├── ShowRepository.java
│   ├── ShowSeatRepository.java
│   ├── BookingRepository.java
│   ├── PaymentRepository.java
│   └── elastic/
│       └── MovieSearchRepository.java  Elasticsearch queries
├── payment/                            ← STRATEGY PATTERN
│   ├── PaymentProcessor.java           Strategy interface
│   ├── PaymentResult.java              Value object
│   ├── RazorpayProcessor.java          Concrete strategy
│   ├── StripeProcessor.java            Concrete strategy
│   ├── PaytmProcessor.java             Concrete strategy
│   └── PaymentProcessorFactory.java    Factory
├── observer/                           ← OBSERVER PATTERN
│   ├── BookingEventObserver.java       Observer interface
│   ├── BookingEventPublisher.java      Subject/Publisher
│   ├── EmailNotificationObserver.java  Concrete observer
│   ├── SMSNotificationObserver.java    Concrete observer
│   └── AnalyticsObserver.java          Concrete observer
├── kafka/
│   ├── BookingEventProducer.java       Kafka producer
│   ├── BookingEventConsumer.java       Kafka consumer
│   └── events/
│       ├── BookingConfirmedEvent.java
│       └── BookingCancelledEvent.java
├── service/
│   ├── UserService.java                Auth & registration
│   ├── MovieService.java               Movie CRUD + ES sync
│   ├── ShowService.java                Show scheduling + seat map
│   ├── SeatLockService.java            Redis seat locking
│   ├── BookingService.java             ← FACADE + TEMPLATE METHOD
│   └── PaymentService.java             Payment orchestration
├── controller/
│   ├── AuthController.java
│   ├── MovieController.java
│   ├── ShowController.java
│   ├── BookingController.java
│   ├── SearchController.java
│   └── TheatreController.java
├── dto/
│   ├── request/                        Input DTOs (validated)
│   └── response/                       Output DTOs
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── SeatAlreadyLockedException.java
│   ├── PaymentFailedException.java
│   └── GlobalExceptionHandler.java     ← Centralized error handling
└── security/
    ├── JwtUtil.java                    JWT create/validate
    ├── JwtFilter.java                  Per-request JWT auth filter
    └── UserDetailsServiceImpl.java     Spring Security user loader
```

