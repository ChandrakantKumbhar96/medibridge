# MediBridge

A full-stack telemedicine platform — patients book and pay for video
consultations, doctors run clinics and issue e-prescriptions, administrators
verify doctors and settle payouts.

**React 18 + Vite + Redux Toolkit + Tailwind** · **Spring Boot 4.1 + Java 21 +
MySQL 8 + Flyway** · Razorpay · Google Sign-In · JWT

---

## What it does

**Patients** search doctors by specialty, book a real slot, pay through Razorpay,
join a video consultation, download prescriptions and reports as PDFs, and rate
the doctor afterwards.

**Doctors** publish weekly availability, run consultations, write structured
e-prescriptions, and track earnings and payouts.

**Administrators** verify medical registrations before a doctor goes live,
monitor platform analytics, configure business rules, and settle doctor payments.

```
book → pay → consult → prescribe → review
```

Paying for a published slot confirms it outright — the model Practo and
Apollo 24|7 use. Cancelling refunds automatically per policy.

---

## Architecture

**Modular monolith** — one deployable, organised into feature modules that talk
through each other's public services, never each other's entities.

```
medibridge-frontend/          React SPA
  src/services/               the only place API URLs live
  src/features/               Redux Toolkit slices
  src/pages/{patient,doctor,admin,public}/

medibridge-backend/           Spring Boot
  com.medibridge/
    common/                   security, config, exceptions, converters
    auth/ patient/ doctor/    ← feature modules
    appointment/ record/
    prescription/ payment/
    payout/ review/ admin/
    notification/ pdf/
  resources/db/migration/     Flyway V1–V5
```

Flyway owns the schema; `ddl-auto: validate` fails startup on entity/schema
drift. See [CONNECTIVITY.md](CONNECTIVITY.md) for the full endpoint map and
[MARKET_ANALYSIS.md](MARKET_ANALYSIS.md) for the competitive feature analysis.

---

## Engineering notes

Things that took more than a tutorial to get right:

**Double booking is prevented by the database, not the application.** A
pessimistic lock and an `is_booked` check make the failure *readable*; the
UNIQUE constraint on `appointment.schedule_id` makes it *impossible*. Two
simultaneous bookings cannot both succeed regardless of timing.

**Prices are snapshotted at booking.** `booked_fee`, `platform_fee` and
`total_amount` are written onto the appointment. A doctor raising their rate
afterwards cannot change what an already-booked patient owes, and the invoice
stays reproducible.

**Payments are verified server-side.** The browser reporting success proves
nothing — the Razorpay signature is recomputed as `HMAC_SHA256(order|payment,
secret)` and compared before anything is marked paid. Forged callbacks are
rejected *and recorded*, in a separate transaction so the audit row survives the
rollback that rejects them.

**Abandoned checkouts self-heal.** A slot is held for 15 minutes; a scheduled
sweep sets `AUTO_EXPIRED` and releases it. Without this, closing the tab would
block that time forever.

**Ownership checks return 404, not 403.** A 403 confirms the resource exists.
Every patient-scoped query is `findByIdAndPatientId` — the caller's identity
comes from the JWT, never a request parameter.

**Doctor earnings are a ledger, not a balance.** One row per consultation with
the commission rate snapshotted. A single balance column would make every
correction a destructive update with no history.

**Reschedule preserves the appointment id**, so the payment, prescription and
audit trail stay attached — no refund-and-recharge cycle. Both slot rows are
locked in ascending id order, because two patients swapping into each other's
slots is otherwise a deadlock.

---

## Running it

**Prerequisites:** Java 21, Maven 3.9+, MySQL 8, Node 18+

```bash
mysql -u root -p -e "CREATE DATABASE medibridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

**Backend** — copy `application-local.yml.example` to `application-local.yml`
and fill in your MySQL password and a JWT secret, then:

```bash
cd medibridge-backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend** — copy `.env.example` to `.env`, then:

```bash
cd medibridge-frontend
npm install && npm run dev
```

Flyway builds the schema and seeds demo data on first run.

| Surface | URL |
|---|---|
| App | http://localhost:5173 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |

**Demo accounts** — password `Test@1234`, admin `Admin@123`

| Role | Email |
|---|---|
| Patient | `john.doe@email.com` |
| Doctor | `sarah.johnson@medibridge.com` |
| Admin | `admin@medibridge.com` |

Razorpay test mode: card `4111 1111 1111 1111` (any future expiry/CVV), or UPI
`success@razorpay`.

The frontend also runs standalone — set `VITE_USE_MOCK=true` and no backend is
needed.

---

## Configuration

Secrets live in gitignored files; `.example` templates are committed.

| Setting | Where | Effect if blank |
|---|---|---|
| MySQL password | `application-local.yml` | app won't start |
| JWT secret | `application-local.yml` | app won't start (min 32 bytes) |
| Razorpay keys | `application-local.yml` | falls back to simulated payments |
| Google client ID | `application-local.yml` + `.env` | Google Sign-In hidden |
| SMTP credentials | `application-local.yml` | emails logged to console |

Business rules — platform fee, slot hold duration, cancellation window, refund
percentage, commission rate, payout cycle — live in the `system_settings` table
and are editable from the admin console without a redeploy.

---

## Status

All features listed above are implemented and verified end to end against a live
MySQL instance and a real Razorpay test account.

**Not done:** automated tests. Verification so far is manual — curl suites, SQL
invariant checks, and browser walkthroughs. `src/test` is empty, so nothing
guards against regressions.

**Future scope:** ABHA/ABDM integration (India's national health ID), family
profiles, lab test booking, in-app chat, SMS notifications.

---

Built as an academic project. Not a licensed medical service.
