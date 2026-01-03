# Travel & Tourism App - Overview

This repository contains a Java-based Travel & Tourism application with:
- a Spring Boot backend (REST API + admin socket)
- a JavaFX desktop frontend (customer + admin UI)

The goal of this README is to explain how the app works from startup through
real user flows so we can understand the system without digging into every file.

## How to Run

Prerequisites:
- JDK 21+, Maven, and PostgreSQL running with credentials matching backend/src/main/resources/application.properties or .env overrides.
- Set any required JWT/DB env vars (see EnvLoader expectations in the backend).

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### Frontend (JavaFX)
```bash
cd frontend
mvn javafx:run
```

## High-Level Architecture

Frontend (JavaFX)  <---- HTTP + JWT ---->  Backend (Spring Boot + PostgreSQL)
Admin UI (JavaFX)  <---- TCP Socket ---->  AdminSocketServer (Spring Boot)

The frontend uses REST for customer features (login, packages, bookings).
Admins use a lightweight TCP socket to manage packages/destinations/hotels.

## Backend: What Runs and Why

Entry point:
- backend/src/main/java/com/travel/loginregistration/LoginRegistrationApplication.java

Startup sequence:
1) EnvLoader loads .env variables (DB, JWT) into Spring properties.
2) Spring Boot starts, auto-configures web + JPA + Flyway.
3) Flyway migrations create/seed tables (see backend/src/main/resources/db/migration).
4) AdminSeeder optionally creates/updates an admin account from .env.
5) AdminSocketServer starts a background thread on port 9090.

Key layers:
- Controller: HTTP endpoints, JSON in/out, no DB access.
- Service: business rules + validation + orchestration.
- Repository: Spring Data JPA for persistence.
- Model: JPA entities mapped to tables.
- DTO: request/response payloads for the API.

JWT security:
- SecurityConfig enables stateless JWT auth.
- JwtAuthenticationFilter reads "Authorization: Bearer <token>".
- /api/auth/* and /api/destinations (GET) are public; everything else requires JWT.

## Frontend: What Loads and How It Navigates

Entry point:
- frontend/src/main/java/com/travel/frontend/MainApp.java

Navigation:
- Navigator keeps one JavaFX Stage and swaps FXML screens (login, home, etc.).
- FXML files live in frontend/src/main/resources/fxml.

Networking:
- ApiClient sends REST requests to http://localhost:8080/api.
- Session stores the JWT and current email.
- DataCache/FileCache speed up repeated reads.

Admin networking:
- AdminSocketClient talks to the backend socket on 127.0.0.1:9090.
- AdminSession holds the admin token for socket commands.

## Main User Flows

1) Login/Register
   - LoginController -> ApiClient.login/register -> /api/auth/*
   - JWT is stored in Session on success

2) Browse Packages
   - PackagesController -> GET /api/packages
   - PackageDetailsController -> GET /api/packages/{id}

3) Book a Package
   - BookingDialogController -> POST /api/bookings
   - BookingService validates profile ID data and creates booking
   - Booking is logged to backend/bookings.log

4) Destinations + Hotels
   - DestinationsController -> GET /api/destinations
   - HotelSearchController -> GET /api/destinations/{id}/hotels
   - HotelDetailsController -> GET /api/destinations/hotels/{hotelId}

5) Book a Hotel Room
   - HotelBookingDialogController -> POST /api/hotels/{hotelId}/rooms/{roomId}/book

6) History + Invoices
   - HistoryController -> GET /api/history
   - InvoiceController -> GET /api/history/invoice/{kind}/{id}

## Admin Flows (Socket + REST)

Admin login:
- LoginController (admin mode) -> AdminSocketClient.auth

Admin package/destination/hotel management:
- AdminDashboardController + AdminDestinationsController + AdminHotelsController
- Uses AdminSocketClient commands (LIST/CREATE/UPDATE/DELETE)

Admin booking oversight:
- REST endpoints under /api/admin/packages/** and /api/admin/rooms/**

## Important Directories

- backend/src/main/java/com/travel/loginregistration/controller
- backend/src/main/java/com/travel/loginregistration/service
- backend/src/main/java/com/travel/loginregistration/model
- backend/src/main/java/com/travel/loginregistration/repository
- backend/src/main/resources/db/migration

- frontend/src/main/java/com/travel/frontend/controller
- frontend/src/main/java/com/travel/frontend/net/ApiClient.java
- frontend/src/main/java/com/travel/frontend/admin/AdminSocketClient.java
- frontend/src/main/resources/fxml

## Notes and Defaults

- Backend runs on port 8080.
- Admin socket runs on port 9090.
- PostgreSQL connection defaults are in backend/src/main/resources/application.properties.
- We can override settings via .env or environment variables.

## Where to Start Reading Code

Backend: LoginRegistrationApplication -> Controllers -> Services -> Repositories
Frontend: MainApp -> Navigator -> controllers (login/home/packages/etc.)
Admin: AdminSocketServer (backend) + AdminSocketClient (frontend)
