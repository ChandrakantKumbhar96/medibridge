# MediBridge API — Endpoint Checking Guide

Swagger UI: **http://localhost:8080/api/swagger-ui.html**
Start the backend first: `mvn spring-boot:run -Dspring-boot.run.profiles=local` (from `medibridge-backend/`).

All paths below are relative to `/api` (Swagger already prefixes it).

## How to authenticate in Swagger

1. `POST /auth/login` with one of the demo accounts below.
2. Copy `token` from the response.
3. Click **Authorize** (top right) → paste the token (no `Bearer` prefix) → **Authorize** → **Close**.
4. Every endpoint below now sends that token. Repeat with a different account to switch roles.

## Demo accounts (seeded by `SampleDataSeeder`)

| Role | Email | Password |
|---|---|---|
| Patient | `aarav.gupta@email.com` / `priya.sharma@email.com` / `rahul.verma@email.com` | `Test@1234` |
| Doctor | `aditya.nair@medibridge.com` / `rohan.mehta@medibridge.com` / `meera.joshi@medibridge.com` | `Test@1234` |
| Doctor (pending, for testing rejection) | `vikram.rao@medibridge.com` | `Test@1234` |
| Admin | `admin@medibridge.com` | `Admin@123` |

`role` in the login body only selects which table to look up (`patient`/`doctor`/`admin`) — it is never trusted as authority, so a mismatched role/email combination must fail with the same generic message as a wrong password.

---

## Auth (`/auth`) — public, no token needed

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/login` | `{ "email", "password", "role" }` | `role`: `patient`/`doctor`/`admin` |
| POST | `/auth/register/patient` | see below | Active immediately, returns a token |
| POST | `/auth/register/doctor` | see below | Returns `201` but `token`/`refreshToken` are `null` — account is `PENDING` until admin approval |
| POST | `/auth/otp/request` | `{ "phone" }` | In `local` profile the code is only logged to the console (no real SMS gateway) |
| POST | `/auth/otp/verify` | `{ "phone", "code" }` | |
| GET | `/auth/providers` | — | Tells the login page whether Google login is configured |
| POST | `/auth/refresh` | `{ "refreshToken" }` | |
| POST | `/auth/logout` | — | Requires Authorize |
| GET | `/auth/me` | — | Requires Authorize; confirms token identity/role |

**Patient register body:**
```json
{
  "full_name": "Test Patient",
  "email": "test.patient@example.com",
  "password": "Test@1234",
  "phone": "+91 90009 99999",
  "date_of_birth": "1995-05-20",
  "gender": "Male",
  "blood_group": "O+"
}
```

**Doctor register body** (`specialization` must match a seeded name: Cardiology, Dermatology, General Physician, Orthopedics, Pediatrics, Neurology):
```json
{
  "full_name": "Dr. Test Kumar",
  "email": "test.doctor@example.com",
  "password": "Test@1234",
  "phone": "+91 90009 88888",
  "specialization": "Cardiology",
  "license_number": "MD-99999-2024",
  "experience_years": 5,
  "consultation_fee": 500,
  "consultation_duration_min": 30
}
```

---

## Doctor discovery (`/doctors`) — public

| Method | Path | Notes |
|---|---|---|
| GET | `/doctors` | Optional `specialization`, `search` query params |
| GET | `/doctors/{doctorId}` | Public profile |
| GET | `/doctors/{doctorId}/slots?date=YYYY-MM-DD` | Feeds `schedule_id` into booking |
| GET | `/specialties` | Cards for the booking wizard |
| GET | `/specializations` | Flat names for the register dropdown |

---

## Patient (`/patient`) — requires `PATIENT` token

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/patient/profile` | — | |
| PUT | `/patient/profile` | profile fields | |
| POST | `/patient/change-password` | `{ "current_password", "new_password" }` | |
| GET | `/patient/stats` | — | Dashboard numbers |
| GET | `/patient/family` | — | List family members |
| POST | `/patient/family` | family member fields | |
| PUT | `/patient/family/{familyMemberId}` | family member fields | |
| DELETE | `/patient/family/{familyMemberId}` | — | Archive, not hard delete |

**Ownership check to try:** as patient A, request another patient's resource (e.g. someone else's family member id) → expect **404**, not 403 or data.

---

## Appointments (`/appointments`)

| Method | Path | Role | Body / Notes |
|---|---|---|---|
| GET | `/appointments/patient` | PATIENT | My appointments (upcoming/past) |
| GET | `/appointments/next-available?specialization=` | PATIENT | |
| POST | `/appointments` | PATIENT | `{ "doctor_id", "schedule_id", "consult_type", "reason", "family_member_id" }` |
| POST | `/appointments/{appointmentId}/follow-up` | PATIENT | `{ "schedule_id" }` — free revisit after a completed consult |
| PATCH | `/appointments/{appointmentId}/cancel` | PATIENT or DOCTOR | |
| PATCH | `/appointments/{appointmentId}/reschedule` | PATIENT or DOCTOR | `{ "schedule_id" }` |
| GET | `/appointments/{appointmentId}/join` | PATIENT or DOCTOR | Video room link |
| GET | `/appointments/{appointmentId}/room-status` | PATIENT or DOCTOR | |
| GET | `/appointments/doctor/dashboard` | DOCTOR | |
| GET | `/appointments/doctor` | DOCTOR | |
| PATCH | `/appointments/{appointmentId}/complete` | DOCTOR | Ends the consult |

**Booking flow to test end-to-end:**
`GET /doctors` → `GET /doctors/{id}/slots?date=` → `POST /appointments` (note the returned `appointment_id`) → pay it (see below) → appointment shows as confirmed on `GET /appointments/patient`.

---

## Payments (`/payments`) — requires `PATIENT` token (refund is `ADMIN`)

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/payments/config` | — | Gateway public config |
| POST | `/payments/order` | `{ "appointment_id", ... }` | Step 1 of gateway flow |
| POST | `/payments/verify` | gateway signature fields | Step 2 |
| POST | `/payments/failed` | `{ "order_id", "reason" }` | |
| POST | `/payments` | `{ "appointment_id", "payment_method": "CARD"\|"UPI"\|"NETBANKING"\|"WALLET" }` | **Simplest to test** — simulated payment, no gateway keys needed |
| GET | `/payments` | — | My payments |
| GET | `/payments/appointment/{appointmentId}` | — | |
| POST | `/payments/{transactionId}/refund` | ADMIN only | |

---

## Records (`/records`) — requires `PATIENT` (download also `DOCTOR`)

| Method | Path | Notes |
|---|---|---|
| GET | `/records` | My uploaded reports |
| POST | `/records` (multipart) | Upload a file — allow-listed content types only, filename is never trusted |
| GET | `/records/{reportId}/download` | PATIENT or DOCTOR |
| DELETE | `/records/{reportId}` | PATIENT |

## Prescriptions (`/prescriptions`)

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/prescriptions` | DOCTOR | Written after a completed appointment |
| GET | `/prescriptions` | PATIENT | My prescriptions |
| GET | `/prescriptions/{prescriptionId}/pdf` | PATIENT or DOCTOR | |
| GET | `/records/medical-history/pdf` | PATIENT | Full history PDF |

## Second opinion (`/opinions`)

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/opinions` | DOCTOR | Create an opinion on a case |
| GET | `/opinions` | PATIENT | My opinions |
| GET | `/opinions/{opinionId}/pdf` | PATIENT or DOCTOR | |

---

## Doctor portal (`/doctor`) — requires `DOCTOR` token

| Method | Path | Notes |
|---|---|---|
| GET | `/doctor/profile` | |
| PUT | `/doctor/profile` | |
| GET | `/doctor/schedule` | Weekly schedule |
| PUT | `/doctor/schedule` | |
| GET | `/doctor/patients` | Patients this doctor has treated |
| GET | `/doctor/patients/{patientId}/records` | Ownership-scoped: only records tied to a shared appointment |
| GET | `/payouts/doctor/earnings/summary` | |
| GET | `/payouts/doctor/earnings` | |
| GET | `/payouts/doctor/payouts` | |

---

## Admin (`/admin`, `/payouts/admin`) — requires `ADMIN` token

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/admin/dashboard` | — | |
| GET | `/admin/patients` | — | All patients |
| GET | `/admin/doctors` | — | All doctors |
| GET | `/admin/appointments` | — | All appointments |
| GET | `/admin/analytics` | — | |
| GET | `/admin/activity?limit=20` | — | |
| PATCH | `/admin/doctors/{doctorId}/status` | `{ "status": "active" }` | **Use this to approve the pending doctor** (`vikram.rao@medibridge.com` or one you registered) |
| PATCH | `/admin/patients/{patientId}/status` | `{ "status": "..." }` | |
| GET | `/admin/settings` | — | |
| PUT | `/admin/settings` | settings fields | Includes cancellation refund policy |
| GET | `/payouts/admin/payouts` | — | |
| GET | `/payouts/admin/payouts/summary` | — | |
| POST | `/payouts/admin/payouts/run` | — | Runs settlement |
| PATCH | `/payouts/admin/payouts/{payoutId}/paid` | — | |
| POST | `/payments/{transactionId}/refund` | — | |

**Approval flow to test:** register a doctor → login fails (pending) → admin `PATCH /admin/doctors/{doctorId}/status` with `{"status":"active"}` → doctor login now succeeds.

---

## Cross-cutting checks worth running for every role

1. **Wrong role, right table**: patient email + `role: doctor` → generic "Invalid email or password", not a distinct error.
2. **Ownership, not just role**: authenticated as patient A, request patient B's resource by id → **404**, never 403 (a 403 would confirm the record exists) and never the data itself.
3. **Token role tampering**: `GET /auth/me` after login always reflects the role of the row actually loaded, not anything sent by the client.
4. **Pending doctor**: cannot log in until `admin` flips status to `active`.
