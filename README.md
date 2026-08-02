# MediBridge

A full-stack telemedicine platform — patients book and pay for video
consultations, doctors run clinics and issue e-prescriptions, administrators
verify doctors and settle payouts.

**React 18 + Vite + Redux Toolkit + Tailwind** · **Spring Boot 4.1 + Java 21 +
MySQL 8 + Flyway** · Node/Express gateway · Python/FastAPI RAG chat · .NET
notify service · Razorpay · Google Sign-In · JWT

---

## What it does

**Patients** search doctors by specialty, book a real slot, pay through Razorpay,
join a video consultation, download prescriptions and reports as PDFs, ask a
symptom-checker chatbot, and rate the doctor afterwards.

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

## What makes this different

Most student projects stop at CRUD. A few things here exist specifically
because a naive version would be wrong, not just unfinished:

- **A monolith with a real microservices slice around it, not a rewrite.**
  The money/booking path (book → pay → confirm) stays a single Spring Boot
  deployable on purpose, so it keeps ACID guarantees. Three satellite
  services in three different languages sit around it for the parts that
  genuinely benefit from being separate — see [Architecture](#architecture).
- **Double booking is prevented by the database, not the application.**
  `appointment.schedule_id` has a UNIQUE constraint. Two simultaneous booking
  requests for the same slot cannot both succeed regardless of timing —
  `doctor_schedule.is_booked` is only a read cache, never the actual guard.
- **A RAG chatbot with its own LLM, not a canned FAQ.** The Python/FastAPI
  service embeds a symptom/FAQ corpus with SentenceTransformer, retrieves
  from ChromaDB, and answers through a Groq-hosted LLM — with guardrails that
  refuse to answer emergency-symptom or out-of-scope questions.
- **Payments are verified server-side, twice.** The browser reporting success
  proves nothing; the Razorpay signature is recomputed as
  `HMAC_SHA256(order|payment, secret)` and compared before anything is marked
  paid. A forged callback is rejected *and logged*, in a transaction separate
  from the one that rejects it, so the audit row survives the rollback.
- **Abandoned checkouts self-heal.** A slot is held 15 minutes; a scheduled
  sweep expires it and releases the slot. Without this, closing the payment
  tab would block that slot forever.
- **Doctor earnings are a ledger, not a balance column.** One row per
  consultation with the commission rate snapshotted at the time. A correction
  is a new row, not a destructive update that erases history.
- **Phone-OTP login auto-registers, but only for patients.** An unrecognised
  number silently becomes a new patient account. Doctors and admins can't use
  it — a doctor account implies a licence a human verified, and a SIM card
  isn't that.
- **The frontend runs standalone with zero backend.** `VITE_USE_MOCK=true`
  flips every service call to an in-memory fixture; the same components,
  same Redux flow, no code branches on which mode is active.

The **why** behind each of these — and every other business rule — is written
up in [docs/BUSINESS_LOGIC.md](docs/BUSINESS_LOGIC.md).

---

## Architecture

Five independently runnable pieces. The core stays a monolith; three small
satellites handle the parts where a different language earns its keep.

```
                         ┌────────────────────┐
                         │   React 18 SPA      │  :5173
                         │  Redux · Tailwind   │
                         └─────────┬───────────┘
                                   │ REST + JWT (Bearer)
                                   ▼
                         ┌────────────────────┐
                         │  Node/Express       │  :4000
                         │  Gateway            │  JWT verify · rate limit ·
                         └──┬────────┬────────┬┘  circuit breaker · proxy
             /api/**        │        │/api/chat         │/api/reports
     (everything else)      │        │/api/triage        │
                             ▼        ▼                    ▼
                  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
                  │ Spring Boot 4.1│ │ Python/FastAPI  │ │ .NET Notify    │
                  │ Java 21 :8080  │ │ Chat/Triage     │ │ Service :5154  │
                  │ modular        │ │ :8000 — RAG     │ │ reminders +    │
                  │ monolith       │ │ (ChromaDB) +    │ │ CSV reports    │
                  │                │ │ Groq LLM        │ │                │
                  └───────┬────────┘ └────────┬────────┘ └───────┬────────┘
                          │ JDBC              (asks Spring          │ polls Spring's
                          ▼                    for doctor/patient    │ /internal/**
                  ┌────────────────┐           context via           │ for due
                  │   MySQL 8      │           X-Internal-Api-Key)   │ reminders
                  │ 26 tables ·    │◄─────────────────────────────────┘
                  │ Flyway V1–V15  │
                  └────────────────┘
```

- **Gateway is the only thing the browser talks to.** It independently
  verifies the Spring-issued JWT (HS384, shared secret) before proxying, so a
  bad or expired token is rejected in ~1ms without ever reaching Spring, and
  adds a per-upstream circuit breaker so one slow downstream can't cascade.
  Everything under `/api` proxies untouched to Spring **except**
  `/api/chat`, `/api/triage` (→ the Python service) and `/api/reports`
  (→ the .NET service) — see [proxy.routes.js](medibridge-gateway/src/routes/proxy.routes.js).
- **`/internal/**` is a separate trust boundary.** Spring's internal
  endpoints and the chat service both require a shared `X-Internal-Api-Key`
  header that only the gateway (and the notify service, for Spring) holds. A
  browser can never reach these directly even if it discovers the URL.
- **Why not full microservices?** Splitting booking/payment/earnings across
  services would trade one transaction for a distributed one, and that's
  exactly where correctness matters most. The satellites exist for pieces
  that don't need that guarantee: chat is stateless Q&A, reminders are
  fire-and-forget, reports are read-only exports.

```
medibridge-frontend/          React SPA
  src/services/                the only place API URLs live
  src/features/                Redux Toolkit slices
  src/pages/{patient,doctor,admin,public}/

medibridge-gateway/           Node/Express — auth gate + reverse proxy
  src/middleware/              auth, rate limit, CORS, request id
  src/proxy/                   upstream proxy + streaming
  src/clients/                 typed clients to Spring/chat/notify

medibridge-backend/           Spring Boot — the modular monolith
  com.medibridge/
    common/                    security, config, exceptions, converters
    auth/ patient/ doctor/     ← feature modules, one package each
    appointment/ opinion/
    record/ prescription/
    payment/ payout/ review/
    admin/ notification/ pdf/
  resources/db/migration/      Flyway V1–V15

medibridge-chat-service/      Python/FastAPI — RAG chat + triage
  app/rag/                     embeddings + ChromaDB ingest
  app/llm/                     Groq client + prompts
  app/guardrails/              emergency + out-of-scope refusal rules

medibridge-notify-service/    .NET 8 — background reminders + reports
  MediBridge.Notify.Jobs/      PeriodicTimer background job
  MediBridge.Notify.Reports/   CSV report generation
```

Flyway owns the schema; `ddl-auto: validate` fails startup on entity/schema
drift. For the full picture — every table with an ER diagram, and every
business rule (booking, pricing, cancellation/refund, reschedule, no-show
settlement, payouts) explained end to end — see
[docs/DATABASE.md](docs/DATABASE.md) and
[docs/BUSINESS_LOGIC.md](docs/BUSINESS_LOGIC.md).

---

## Who does what

**Backend feature modules** — each is a package under `com.medibridge`, one
`Service` per module. The rule that keeps the monolith modular: *a module may
import another module's `Service` and `dto`, never its `entity` or
`repository`.* `auth` is the one exception, since login inherently spans all
three identity tables.

| Module | Responsibility |
|---|---|
| `auth` | login (all 3 roles), registration, JWT issue/refresh, Google Sign-In, phone OTP |
| `patient` | profile, password change, dashboard stats, family/dependent profiles |
| `doctor` | search/listing, weekly availability, schedule, profile, qualifications |
| `appointment` | booking, cancel, reschedule, no-show settlement, slot-expiry scheduler |
| `opinion` | second opinion as a standalone document, separate from a live consult |
| `record` | medical report upload/download — UUID filename, allow-listed content type |
| `prescription` | consultation records, structured e-prescriptions, drug interaction checks |
| `payment` | Razorpay orders, HMAC signature verification, refunds |
| `payout` | doctor earnings ledger, payout batch settlement |
| `review` | ratings, highlights, doctor rating recalculation |
| `admin` | dashboards, analytics, doctor approval, user management, system settings |
| `notification` | email + WhatsApp sends, meeting-link generation |
| `pdf` | Thymeleaf → PDF rendering for prescriptions and reports |
| `common` | JWT/security, config, exception handling, enum ↔ MySQL-ENUM converters |

**The other four pieces:**

| Service | Responsibility |
|---|---|
| **Frontend (React)** | three role-scoped route trees (`/patient`, `/doctor`, `/admin`), Redux slices per feature, a service layer that's the only place that knows whether it's talking to a mock or the real API |
| **Gateway (Node)** | single entry point for the browser — JWT verification, rate limiting, circuit breaker, reverse proxy to the other three |
| **Chat/Triage (Python)** | symptom-checker and FAQ chat — RAG retrieval over an embedded corpus, answered by an LLM, with emergency/out-of-scope guardrails |
| **Notify (.NET)** | polls Spring for appointments due a reminder, sends via Twilio SMS, generates CSV reports for the admin dashboard on demand |

---

## How a request flows

**Example: booking and paying for a slot**, end to end.

```
Patient picks a doctor, date, slot
  → FE: dispatch(bookAppointment())            (Redux thunk)
  → FE: appointmentService.book()               (services/appointmentService.js)
  → Gateway :4000                                JWT verified, rate limiter passed
  → Spring AppointmentController                 @CurrentUser from the JWT, never a request param
  → AppointmentService.book()  (@Transactional)
      · lock the slot row (findByIdForUpdate)
      · reject if already booked
      · snapshot booked_fee + platform_fee onto the appointment
      · INSERT appointment, status = PENDING_PAYMENT
      · UNIQUE(schedule_id) is the final, race-proof guard
  ← appointment_id back to the browser

Patient pays
  → FE: POST /payments/order
  → PaymentService creates a Razorpay order — amount is read from the
    server-side snapshot, never trusted from the client
  → Razorpay Checkout opens in the browser, patient pays (test card in dev)
  → FE: POST /payments/verify  { payment_id, order_id, signature }
  → PaymentService recomputes HMAC_SHA256(order|payment, secret) and compares
  → on match: appointment → ACCEPTED, meeting link minted, confirmation
    email/WhatsApp sent (via the notification module)
  ← confirmed appointment back to the browser
```

Every layer has one job: `Controller` (HTTP + `@PreAuthorize` role checks) →
`Service` (business rules, `@Transactional`, **ownership checks** —
`findByIdAndPatientId`, never a bare `findById`) → `Repository` → MySQL.
Cross-module side effects go through **Spring events**
(`AppointmentCompleted` → `payout` module accrues a `DoctorEarning`), so the
publisher never imports the listener's module.

**On the frontend**, every page follows the same shape — a page component
never calls `axios` directly:

```
Page (e.g. FindDoctors.jsx)
  → dispatch(fetchDoctors())          fired from useEffect on mount
  → Redux thunk (doctorsSlice.js)     createAsyncThunk, sets status: 'loading'
  → services/doctorService.js         if (USE_MOCK) return the fixture;
                                       else axiosClient.get('/doctors')
  → axiosClient                       interceptor attaches
                                       Authorization: Bearer <mb_token>
  → Gateway → Spring → MySQL → JSON
  → slice reducer                     status: 'succeeded', state updated
  → component re-renders via useSelector
```

`USE_MOCK` (from `src/api/axiosClient.js`, default **true** unless the env
string is exactly `"false"`) is the one flag that decides mock vs. live for
every service method. Components never see it.

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

**Prerequisites:** Java 21, Maven 3.9+, MySQL 8, Node 18+ — plus Python 3.11+
and .NET 8 SDK if you want the chat and notify services running too.

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

Flyway builds the schema and seeds demo data on first run. The frontend talks
to the gateway by default (`VITE_GATEWAY_API_URL`, falls back to
`http://localhost:4000/api`) — start the gateway too, or point that variable
straight at Spring's `:8080/api` if you'd rather skip it for local work.

**Optional satellites:**

```bash
# Gateway — required for the frontend's default config
cd medibridge-gateway && npm install && npm run dev        # :4000

# Chat/Triage — only needed for the symptom checker / chat widget
cd medibridge-chat-service && uvicorn app.main:app --reload # :8000

# Notify — only needed for SMS reminders / CSV report export
cd medibridge-notify-service/src/MediBridge.Notify.Api && dotnet run  # :5154
```

| Surface | URL |
|---|---|
| App | http://localhost:5173 |
| Gateway | http://localhost:4000/api |
| Spring API | http://localhost:8080/api |
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
| Groq API key | chat-service `.env` | chat/triage returns a fallback message |
| Twilio credentials | notify-service config | SMS reminders logged, not sent |

Business rules — platform fee, slot hold duration, cancellation window, refund
percentage, commission rate, payout cycle — live in the `system_settings` table
and are editable from the admin console without a redeploy.

---

## Status

Core patient/doctor/admin flows, payments, and the gateway/chat/notify
satellites are implemented and verified end to end against a live MySQL
instance and a real Razorpay test account.

**Tested:** an integration test suite (`medibridge-backend/src/test`) runs
against a real MySQL schema — concurrent booking races, cancellation/refund
policy, reschedule rules, no-show settlement, phone-OTP login, and API-level
security checks. Run with `mvn test -Dspring-boot.run.profiles=local` from
`medibridge-backend/` (`@ActiveProfiles({"local", "test"})` layers
`application-test.yml` over the local DB config); not run as part of a normal
edit loop.

**Future scope:** ABHA/ABDM integration (India's national health ID), medicine
ordering, lab test booking, insurance integration, a mobile app.

---

**More docs:** [docs/](docs/) has the deep dives — full schema, every business
rule end to end, and a presentation write-up with sequence/state/ER diagrams.
[MARKET_ANALYSIS.md](MARKET_ANALYSIS.md) maps this against Practo, Apollo
24|7, 1mg and PharmEasy feature-by-feature.

---

Built as an academic project. Not a licensed medical service.
