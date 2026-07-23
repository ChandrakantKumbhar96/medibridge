# MediBridge — Backend Documentation

Spring Boot 4.1 REST API for the MediBridge telemedicine platform.

---

## 1. Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.1.0 (Spring Framework 7) |
| Security | Spring Security 7 — JWT access + refresh tokens, BCrypt, Google OAuth2 |
| Persistence | Spring Data JPA / Hibernate 7 |
| Database | MySQL 8 |
| Migrations | Flyway |
| Build | Maven |
| Payments | Razorpay Java SDK |
| PDF | Thymeleaf + openhtmltopdf |
| Email | Spring Mail (JavaMail) |
| API docs | springdoc-openapi (Swagger UI) |
| Boilerplate | Lombok |

---

## 2. Architecture

**Modular monolith** — a single deployable unit organised into feature modules,
not microservices. Each module owns its own controllers, services, repositories,
entities, and DTOs.

```
com.medibridge
├── MediBridgeApplication.java      # entry point (@EnableScheduling, @EnableAsync, @EnableJpaAuditing)
│
├── common/                         # shared across all modules
│   ├── config/                     # SecurityConfig, OpenApiConfig, WebConfig, DataSeeder, SampleDataSeeder
│   ├── security/                   # JwtService, JwtAuthFilter, SecurityUser, @CurrentUser
│   ├── exception/                  # GlobalExceptionHandler + custom exceptions
│   └── enums/                      # Role, AppointmentStatus, PaymentStatus + JPA converters
│
├── auth/                           # login, registration, token refresh, Google OAuth
├── patient/                        # patient profile, password change, stats
├── doctor/                         # doctor listing/search, availability, schedule, profile
├── appointment/                    # booking, cancel, reschedule, complete, dashboards, scheduler
├── record/                         # medical report upload/download, file storage
├── prescription/                   # consultation records, prescriptions, PDF data
├── payment/                        # Razorpay orders, verification, refunds
├── payout/                         # doctor earnings ledger + settlement
├── review/                         # ratings and highlights
├── admin/                          # dashboard, analytics, settings, activity log, user management
├── notification/                   # email + meeting-link generation
└── pdf/                            # Thymeleaf → PDF rendering engine
```

### The module rule

> A module may import another module's **`Service`** and **`dto`**, never its
> **`entity`** or **`repository`**.

This keeps boundaries real. `auth` is the one documented exception —
authentication inherently spans the three identity tables.

### Layered flow inside a module

```
HTTP request
  → @RestController        input validation, auth annotations
  → @Service               business rules, @Transactional, ownership checks
  → JpaRepository          data access
  → MySQL
```

### Cross-module communication: events

Modules that must react to each other's actions use **Spring application events**
rather than direct calls, so the publisher stays unaware of the listener:

| Event | Published by | Listener | Effect |
|---|---|---|---|
| `AppointmentCompletedEvent` | appointment | payout | accrue doctor earning |
| `AppointmentCancelledEvent` | appointment | payment | automatic refund |
| `PaymentRefundedEvent` | payment | payout | reverse doctor earning |
| `LoginEvent` / `LogoutEvent` | auth | admin | write audit-log row |

All listeners use `@TransactionalEventListener` (AFTER_COMMIT) so side effects
never fire for a transaction that rolls back.

---

## 3. Project counts

| Item | Count |
|---|---|
| Feature modules | 13 |
| REST controllers | 12 |
| Service classes | 17 |
| JPA repositories | 18 |
| Entities | 19 |
| Database tables | 20 |
| Flyway migrations | 5 (V1–V5) |
| REST endpoints | 50+ |

---

## 4. Security

### Authentication — stateless JWT

- Login returns `{ token, refresh_token, user }`.
- **Access token**: 15-minute lifetime, HS256-signed, carries id/email/name/role
  as claims — so authenticated requests need no DB lookup.
- **Refresh token**: 7 days, stored **hashed** (SHA-256) in `refresh_token`.
  Rotated on every use; logout revokes it.
- `JwtAuthFilter` reads `Authorization: Bearer <token>` and populates the
  security context.

### Three identity tables

`patient`, `doctor`, `admin` — each with its own `password_hash`. Login sends a
`role` to select the table, **but that role is never trusted as authority** — it
only picks where to look; authority comes from the row actually loaded.

### Authorization — two layers

1. **URL rules** (`SecurityConfig`) — coarse: `/admin/**` → `ROLE_ADMIN`, etc.
2. **`@PreAuthorize`** on methods — role-level.
3. **Ownership checks in the service layer** — the important one. `hasRole('PATIENT')`
   cannot express *"only their own records"*, so every patient-scoped query is
   `findByIdAndPatientId(...)`, taking the caller id from `@CurrentUser`.

### Security invariants (do not weaken)

- **Return 404, not 403, on ownership failure** — a 403 confirms the resource exists.
- **Login failures say only "Invalid email or password"** — anything more enumerates accounts.
- **Double booking** is prevented by the `UNIQUE` constraint on
  `appointment.schedule_id`, not by an application check.
- **Uploaded files** get a UUID filename and an allow-listed content type; the
  user's filename is never used as a path.
- **Payment signatures** are verified server-side (HMAC-SHA256); a forged
  callback is rejected and recorded as `FAILED`.

### Google OAuth2

`POST /auth/google` accepts a Google ID token, verifies it against Google's JWKS
(checking the `aud` claim matches our client id), then finds-or-creates a patient.

---

## 5. Key business rules

### Booking model — slot-based confirmation

Publishing a slot is the doctor's agreement to be booked, so **paying for a slot
confirms the appointment outright** (Practo/Apollo model). No accept/reject step.

```
book → PENDING_PAYMENT (slot held for 15 min)
pay  → ACCEPTED (meeting link generated + emailed)
consult happens
complete / prescribe → COMPLETED (doctor earning accrued)
review → doctor rating recalculated
```

- **Abandoned checkout**: a scheduled job (`AppointmentScheduler`, every 60s)
  sets `AUTO_EXPIRED` and releases the slot.
- **Cancellation**: automatic refund — full if a doctor cancels or a patient
  cancels outside the free window, partial inside it.
- **Reschedule**: keeps the same appointment id (so payment carries over); locks
  both slot rows in ascending id order to avoid deadlock.

### Money integrity

- Fee + platform fee are **snapshotted onto the appointment at booking**, so a
  later price change cannot alter what a booked patient owes.
- The gateway charges the snapshotted total, never a client-supplied amount.
- **Doctor payout ledger**: one `doctor_earning` row per completed consultation
  (gross − commission = net). Settled in batches per period. Refunds reverse the
  earning.

### Configurable policy

Platform fee, slot-hold minutes, cancellation window, refund %, commission %, and
payout cycle all live in `system_settings` — changeable without a redeploy.

---

## 6. Configuration & running

### Required: the `local` profile

The app **must** run with the `local` profile, which loads
`application-local.yml` (gitignored) containing the DB password, JWT secret,
Razorpay keys, and Google client id.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

In an IDE, set the active profile to `local` (or env var `SPRING_PROFILES_ACTIVE=local`).

### First-run behaviour

- Flyway applies V1–V5, creating all 20 tables.
- `DataSeeder` creates the demo admin.
- `SampleDataSeeder` loads 6 doctors, 4 patients, appointments, prescriptions,
  reviews (idempotent — skipped if doctors already exist).

### Endpoints

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Patient | john.doe@email.com | Test@1234 |
| Doctor | sarah.johnson@medibridge.com | Test@1234 |
| Admin | admin@medibridge.com | Admin@123 |

---

## 7. "Things that will bite you" (Spring Boot 4 gotchas)

- **`flyway-core` alone does nothing in Boot 4** — autoconfiguration was split
  out. You need `org.springframework.boot:spring-boot-flyway`.
- **`ddl-auto: validate` is deliberate** — it catches entity/schema drift at
  startup (it caught `Short`→SMALLINT vs a TINYINT column twice).
- **Java enums vs MySQL ENUM literals need converters** — the DB holds
  `'active'` and `'Bedside Manner'`, not `ACTIVE`. See `common/enums/converter/`.
- **JSON casing is mixed on purpose** — entity fields snake_case, admin
  aggregates camelCase — via explicit `@JsonProperty`, not a global strategy.
- **`@EnableScheduling` is required** for the slot-expiry and reminder jobs;
  without it every `@Scheduled` method is silently inert.

See the full REST reference in [API_REFERENCE.md](API_REFERENCE.md) and the
schema in [DATABASE.md](DATABASE.md).
