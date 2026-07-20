# RentalCarProject

A comprehensive **Rental Car Booking REST API** built with **Java 21**, **Spring Boot 3.3.5**, and **H2 in-memory database**.

---

## Features

### Customer Features
| Feature | Description |
|---|---|
| Registration & Login | Email/Password, Google, Facebook, Apple, OTP-based |
| Vehicle Search | By city, dates, car type, fuel type, transmission, brand, price range, seats |
| Car Details | Images, specs, price, ratings, availability |
| Booking System | Instant, scheduled, one-way, round-trip, multiple pickup locations |
| Payments | Credit/Debit card, UPI, Net Banking, Digital Wallets, EMI |
| Booking Management | View history, modify, cancel, download invoice |
| Reviews & Ratings | Vehicle ratings, driver ratings |

### Admin Features
| Feature | Description |
|---|---|
| Dashboard | Total bookings, revenue, vehicle utilization |
| Vehicle Management | Add, edit, delete, manage availability & pricing |
| Customer Management | User profiles, booking records, KYC review |
| Booking Management | Approve, complete, cancel, process refunds |

### Security Features
- JWT-based stateless authentication (HS512)
- Role-based access control (CUSTOMER / ADMIN)
- KYC verification via driving license upload
- Input validation on all request bodies
- Structured logging with sensitive data masking
- H2 console restricted to localhost

---

## Tech Stack
- **Java 21**
- **Spring Boot 3.3.5** (Web, Data JPA, Security, OAuth2 Client, Validation, Mail, Actuator)
- **H2** in-memory database
- **JWT** (jjwt 0.12.6, HS512)
- **Lombok** + **MapStruct**
- **SpringDoc OpenAPI 2.6.0** (Swagger UI)

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Run
```bash
cd "Rental car project"
mvn spring-boot:run
```

### Access Points
| URL | Description |
|---|---|
| `http://127.0.0.1:8080/swagger-ui.html` | Interactive API documentation |
| `http://127.0.0.1:8080/h2-console` | H2 database console (JDBC URL: `jdbc:h2:mem:rentalcardb`) |
| `http://127.0.0.1:8080/actuator/health` | Health check |

### Default Seed Credentials
| Role | Email | Password |
|---|---|---|
| Admin | admin@rentalcar.com | Admin@12345 |
| Customer | john.doe@example.com | Customer@12345 |

---

## API Overview

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Email/password registration |
| POST | `/login` | Email/password login |
| POST | `/otp/send` | Send OTP to phone |
| POST | `/otp/verify` | Verify OTP and get token |
| POST | `/social` | Google / Facebook / Apple login |

### Vehicles (`/api/v1/vehicles`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/search` | Search with filters |
| GET | `/{id}` | Vehicle detail page |
| GET | `/cities` | All available cities |
| GET | `/brands` | All available brands |

### Bookings (`/api/v1/bookings`) 🔒
| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create booking |
| GET | `/` | My booking history |
| GET | `/{reference}` | Booking detail |
| PUT | `/{id}` | Modify booking |
| DELETE | `/{id}/cancel` | Cancel booking |

### Payments (`/api/v1/payments`) 🔒
| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Initiate payment |
| GET | `/booking/{bookingId}` | Payment status |

### Reviews (`/api/v1/reviews`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Submit review 🔒 |
| GET | `/vehicle/{vehicleId}` | Vehicle reviews |
| GET | `/my` | My reviews 🔒 |

### Admin (`/api/v1/admin`) 🔒 ADMIN only
Full CRUD for vehicles, users, bookings, KYC approval, refunds, and review moderation.

---

## Exception Handling

| Exception | HTTP Status | Message |
|---|---|---|
| `DuplicateEmailException` | 409 Conflict | "This email is already registered..." |
| `InvalidCredentialsException` | 401 Unauthorized | "Invalid credentials. Please try again." |
| `NoVehiclesAvailableException` | 404 Not Found | "No vehicles available for your selected period." |
| `VehicleNotAvailableException` | 409 Conflict | "The selected vehicle has already been reserved..." |
| `InvalidOtpException` | 400 Bad Request | "Invalid OTP. Please check and try again." |
| `OtpExpiredException` | 400 Bad Request | "OTP has expired. Please request a new one." |
| `BookingModificationException` | 400 Bad Request | Context-specific message |
| `ResourceNotFoundException` | 404 Not Found | "{Resource} not found with {field}: '{value}'" |

---

## Project Structure
```
src/main/java/com/rentalcar/booking/
├── RentalCarApplication.java
├── config/
│   ├── DataInitializer.java
│   └── OpenApiConfig.java
├── controller/
│   ├── AdminController.java
│   ├── AuthController.java
│   ├── BookingController.java
│   ├── PaymentController.java
│   ├── ReviewController.java
│   ├── UserController.java
│   └── VehicleController.java
├── dto/
│   ├── request/  (RegisterRequest, LoginRequest, CreateBookingRequest, ...)
│   └── response/ (AuthResponse, BookingResponse, VehicleResponse, ...)
├── entity/
│   ├── User.java
│   ├── Vehicle.java
│   ├── Booking.java
│   ├── Payment.java
│   ├── Review.java
│   ├── AuditLog.java
│   ├── VehicleImage.java
│   └── VehicleAvailability.java
├── enums/
│   └── (AuthProvider, BookingStatus, CarType, FuelType, ...)
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── (domain-specific exceptions)
├── repository/
│   └── (JPA repositories)
├── security/
│   ├── SecurityConfig.java
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthEntryPoint.java
│   └── CustomUserDetailsService.java
└── service/
    ├── AdminService.java
    ├── AuthService.java
    ├── BookingService.java
    ├── PaymentService.java
    ├── ReviewService.java
    ├── UserService.java
    └── VehicleService.java
```

---

## Security Notes
- JWT secret must be at least 512 bits — set via `JWT_SECRET` environment variable in production
- Passwords are BCrypt-hashed with strength 12
- No secrets or credentials are committed — use environment variables
- Server binds to `127.0.0.1` (not `0.0.0.0`) by default
- H2 console is disabled for external access
