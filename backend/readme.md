# Travel & Tourism Backend Overview

This document explains how the backend module is organised and how a request moves through the system. Keep it nearby when tracing features or updating the API.

---

## 1. Architecture Summary
- **Spring Boot 3** application started by `LoginRegistrationApplication`.
- **Layered design**: controllers → services → repositories → database.
- **DTOs** carry every request and response so database entities stay hidden inside services.
- **PostgreSQL** schema managed by Flyway migrations.
- **JWT authentication** protects every endpoint except the public auth routes.
- **Admin socket** exposes a TCP interface for administrator-only package maintenance.

---

## 2. Application Startup
1. `LoginRegistrationApplication` runs and invokes `EnvLoader` to map values from `.env` into Spring properties (datasource, Flyway, JWT).
2. Spring Boot auto-configures the web server, JPA, Flyway, and component scanning for `com.travel.loginregistration`.
3. Flyway migrations in `backend/src/main/resources/db/migration` ensure the schema exists and seed base data (packages).
4. `AdminSeeder` executes once to create or update the default admin account if the relevant `.env` variables are present.
5. `AdminSocketServer` starts a background thread listening on TCP port 9090 for admin operations.

---

## 3. Request Lifecycle
```
Frontend JSON
   ↓ (Spring Web)
Controller  →  DTO binding
   ↓
Service (business logic, validation)
   ↓
Repository (Spring Data JPA)
   ↓
Database entities
   ↑
Service → DTO response
   ↑
Controller → JSON response
```

Controllers never touch repositories directly, and services never return entity instances to the web layer. DTOs are the contract for every API.

---

## 4. Controllers
| Controller | Route prefix | Purpose |
|------------|--------------|---------|
| `AuthController` | `/api/auth` | Register and log in users, returning JWT tokens. |
| `ProfileController` | `/api/profile` | Fetch and update the authenticated user’s profile (`/me`). |
| `PackageController` | `/api/packages` | List active packages and fetch per-package details. |
| `BookingController` | `/api/bookings` | Create bookings for the logged-in user when their profile is complete. |

Each controller simply validates top-level inputs, delegates to its service, and converts service responses into HTTP responses.

---

## 5. Services
- **AuthService**: validates credentials, hashes passwords, checks for duplicate emails, and generates JWTs via `JwtUtil`.
- **ProfileService**: synchronises `app_users` (base account) and `user_profiles` records and enforces allowed `idType`/`gender` values.
- **PackageService**: reads travel packages through `TravelPackageRepository` and maps them into `PackageSummary` or `PackageDetails` DTOs.
- **BookingService**: ensures eligibility (ID info must exist), loads the referenced `TravelPackage`, calculates total cost, persists a `Booking`, and appends an audit line to `backend/bookings.log`.

Services encapsulate all business rules and orchestrate repository calls inside transactions.

---

## 6. Repositories and Models
- Entities in `backend/src/main/java/com/travel/loginregistration/model` map directly to the Flyway-created tables (`app_users`, `travel_packages`, `bookings`, `admin_users`, `user_profiles`).
- Repositories (Spring Data interfaces) provide CRUD and query helpers (e.g., `findByEmail`, `findByActiveTrueOrderByNameAsc`, `findByUserId`).
- Services rely on these repositories for all persistence operations; no service issues SQL manually.

---

## 7. DTOs (Data Transfer Objects)
- Request DTOs: `RegisterRequest`, `LoginRequest`, `BookingRequest`, `ProfileUpdateRequest`.
- Response DTOs: `ProfileResponse`, `PackageSummary`, `PackageDetails`, `BookingResponse`.
- DTOs are used in controller method signatures and returned to clients, keeping the REST contract stable even if the database model changes.

---

## 8. Security and Admin Channel
- `SecurityConfig` disables CSRF for APIs, configures stateless sessions, exposes `/api/auth/*` and `/actuator/health` as public, and requires JWT authentication elsewhere.
- `JwtAuthenticationFilter` inspects the `Authorization: Bearer <token>` header, validates it via `JwtUtil`, and populates the security context with the user’s email.
- The admin desktop client does not rely on REST for package maintenance. Instead, `AdminSocketServer` authenticates administrator credentials, issues session tokens, and handles `LIST/CREATE/UPDATE/DELETE` commands over a TCP socket. `AdminSocketClient` in the JavaFX frontend implements the same protocol.

---

## 9. Database and Environment
- `.env` holds connection strings, credentials, and JWT secrets. `EnvLoader` maps keys like `SPRING_DATASOURCE_URL` and `APP_JWT_SECRET` to Spring property names.
- Flyway migration files `V1__...V7__...` create every table, indexes, and seed data for travel packages.
- `bookings.log` is a simple append-only audit file written by `BookingService.logToFile`.

---

## 10. Example: Booking Flow
1. JavaFX `BookingDialogController` POSTs `{ "packageId": "...", "totalPersons": 2 }` to `/api/bookings` with a valid JWT.
2. Spring binds the JSON body to `BookingRequest` and injects the authenticated email into `BookingController.book`.
3. `BookingService.book` validates inputs, loads the `User`, `TravelPackage`, and `UserProfile`, enforces eligibility, calculates total price, saves a `Booking`, logs the booking, and builds a `BookingResponse`.
4. The controller wraps the response in `ResponseEntity.ok(...)` and Spring serialises it to JSON for the frontend.

Understanding this single flow covers the common pattern used by every endpoint in the backend.
