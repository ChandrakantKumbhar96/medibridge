# MediBridge — REST API Reference

Base URL: `http://localhost:8080/api`
Interactive docs: `http://localhost:8080/api/swagger-ui.html`

**Auth:** all endpoints except those marked *public* require
`Authorization: Bearer <access-token>`. Obtain one from `POST /auth/login`.

**Roles:** 🟢 Patient · 🔵 Doctor · 🔴 Admin · ⚪ Public

---

## Authentication

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/auth/login` | ⚪ | Log in; body `{ email, password, role }` → `{ token, refresh_token, user }` |
| POST | `/auth/register/patient` | ⚪ | Register a patient; returns a session immediately |
| POST | `/auth/register/doctor` | ⚪ | Register a doctor; **no token** — status is `pending` until admin approval |
| POST | `/auth/google` | ⚪ | Log in with a Google ID token (verified server-side) |
| POST | `/auth/refresh` | ⚪ | Exchange a refresh token for a new token pair |
| POST | `/auth/logout` | any | Revoke the caller's refresh token(s) |
| GET | `/auth/me` | any | Current user from the token |
| GET | `/auth/providers` | ⚪ | Which login providers are enabled (e.g. Google) |

---

## Doctors & specialties (discovery)

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/doctors` | 🟢 | List active doctors; `?specialization=&search=` |
| GET | `/doctors/{id}` | 🟢 | One doctor's public profile |
| GET | `/doctors/{id}/slots?date=` | 🟢 | Free slots for a date (generated on demand) |
| GET | `/doctors/{id}/reviews` | ⚪ | Reviews for a doctor |
| GET | `/specialties` | ⚪ | Specialty cards (name, emoji, doctor count) |
| GET | `/specializations` | ⚪ | Flat specialization names (for dropdowns) |

---

## Appointments

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/appointments/patient` | 🟢 | Caller's appointments, split `upcoming` / `past` |
| POST | `/appointments` | 🟢 | Book a slot → status `PENDING_PAYMENT` |
| PATCH | `/appointments/{id}/cancel` | 🟢🔵 | Cancel; body `{ reason }`; triggers auto-refund |
| PATCH | `/appointments/{id}/reschedule` | 🟢🔵 | Move to another slot; body `{ schedule_id }` |
| GET | `/appointments/doctor/dashboard` | 🔵 | Buckets: today / upcoming / awaiting-notes / completed |
| GET | `/appointments/doctor` | 🔵 | All the doctor's appointments |
| PATCH | `/appointments/{id}/complete` | 🔵 | Mark a consultation complete (time must have passed) |

---

## Payments

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/payments/config` | 🟢 | Whether the real gateway is enabled |
| POST | `/payments/order` | 🟢 | Create a Razorpay order (amount fixed server-side) |
| POST | `/payments/verify` | 🟢 | Verify the gateway signature; confirms the appointment |
| POST | `/payments/failed` | 🟢 | Record an abandoned/failed attempt |
| POST | `/payments` | 🟢 | Simulated payment (when no gateway keys configured) |
| GET | `/payments` | 🟢 | Caller's payment history |
| POST | `/payments/{txnId}/refund` | 🔴 | Manual full refund (calls the gateway) |

---

## Prescriptions & records

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/prescriptions` | 🔵 | Record consultation + issue prescription; completes the appointment |
| GET | `/prescriptions` | 🟢 | Caller's prescriptions |
| GET | `/prescriptions/{id}/pdf` | 🟢🔵 | Download the prescription as PDF |
| GET | `/records` | 🟢 | Caller's uploaded medical documents |
| POST | `/records` | 🟢 | Upload a document (multipart) |
| GET | `/records/{id}/download` | 🟢🔵 | Download a document |
| DELETE | `/records/{id}` | 🟢 | Delete a document |
| GET | `/records/medical-history/pdf` | 🟢 | Full medical-history PDF |

---

## Reviews

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/reviews` | 🟢 | Submit a rating (stars, experience, multi-select highlights) |
| GET | `/reviews/appointment/{id}` | 🟢 | The review for one appointment |

---

## Patient self-service

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/patient/profile` | 🟢 | Own profile |
| PUT | `/patient/profile` | 🟢 | Update own profile |
| POST | `/patient/change-password` | 🟢 | Change password (revokes refresh tokens) |
| GET | `/patient/stats` | 🟢 | Overview tile counts |

---

## Doctor self-service & earnings

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET / PUT | `/doctor/profile` | 🔵 | View / update own profile |
| GET / PUT | `/doctor/schedule` | 🔵 | Weekly availability pattern |
| GET | `/doctor/patients` | 🔵 | Patients the doctor has treated |
| GET | `/doctor/patients/{id}/records` | 🔵 | A treated patient's documents |
| GET | `/doctor/earnings` | 🔵 | Per-consultation earnings ledger |
| GET | `/doctor/earnings/summary` | 🔵 | Pending / paid / lifetime totals |
| GET | `/doctor/payouts` | 🔵 | Own payout batches |

---

## Admin

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/admin/dashboard` | 🔴 | Stats + recent activity |
| GET | `/admin/patients` | 🔴 | All patients |
| GET | `/admin/doctors` | 🔴 | All doctors |
| PATCH | `/admin/doctors/{id}/status` | 🔴 | Approve / suspend / reinstate a doctor |
| PATCH | `/admin/patients/{id}/status` | 🔴 | Activate / deactivate a patient |
| GET | `/admin/appointments` | 🔴 | All appointments |
| GET | `/admin/analytics` | 🔴 | Monthly stats, revenue, daily revenue |
| GET / PUT | `/admin/settings` | 🔴 | System settings (fees, windows, policy) |
| GET | `/admin/activity` | 🔴 | Audit-log feed |
| GET | `/admin/payouts` | 🔴 | All payout batches |
| GET | `/admin/payouts/summary` | 🔴 | Platform commission / owed / paid |
| POST | `/admin/payouts/run` | 🔴 | Create payout batches for a period |
| PATCH | `/admin/payouts/{id}/paid` | 🔴 | Record that a transfer was made |

---

## Standard error shape

Every error returns the same JSON, which the frontend reads as
`err.response.data.message`:

```json
{ "message": "Invalid email or password", "status": 401, "timestamp": "..." }
```

| Status | Meaning in this API |
|---|---|
| 400 | Validation / malformed body / bad parameter |
| 401 | Missing, invalid, or expired token; bad credentials |
| 403 | Authenticated but wrong role |
| 404 | Not found — **also returned on ownership failure** (a 403 would confirm existence) |
| 405 | Wrong HTTP method |
| 409 | Conflict — duplicate email, slot already taken, already paid/refunded |
| 413 | Upload exceeds 10 MB |
