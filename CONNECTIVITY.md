# MediBridge — Frontend ↔ Backend Connectivity

Complete map of how the React app talks to the Spring Boot API.

- **Frontend** http://localhost:5173 (Vite)
- **Backend** http://localhost:8080/api (Spring Boot 4.1, context path `/api`)
- **Swagger** http://localhost:8080/api/swagger-ui.html
- **Database** MySQL `medibridge`, schema owned by Flyway (V1–V4)

---

## How a request travels

```
page component
  → dispatch(thunk)              features/*/slice.js
  → service method               services/*Service.js      ← the only place URLs live
  → axiosClient                  api/axiosClient.js        ← attaches JWT, retries on 401
  → HTTP                         localhost:8080/api/...
  → @RestController              com.medibridge.<module>
  → @Service                     business rules + ownership checks
  → JpaRepository                MySQL
```

**Two rules that keep this honest:**

1. Components never call `axios` directly — only `services/*`. Switching to mock
   data is one env flag (`VITE_USE_MOCK=true`) because of this.
2. The caller's identity always comes from the JWT (`@CurrentUser`), never from a
   request parameter. That is what stops one patient reading another's records.

---

## Authentication

| Frontend | Method | Endpoint | Notes |
|---|---|---|---|
| `authService.login` | POST | `/auth/login` | `role` picks which table to check — it grants nothing |
| `authService.registerPatient` | POST | `/auth/register/patient` | returns a session immediately |
| `authService.registerDoctor` | POST | `/auth/register/doctor` | **no token** — pending admin approval |
| `authService.loginWithGoogle` | POST | `/auth/google` | ID token verified server-side against Google's JWKS |
| `authService.getProviders` | GET | `/auth/providers` | tells the UI whether to show the Google button |
| *(axiosClient interceptor)* | POST | `/auth/refresh` | automatic — see below |

**Token lifecycle.** The access token lasts 15 minutes. On any 401 the interceptor
in [axiosClient.js](medibridge-frontend/src/api/axiosClient.js) exchanges the
refresh token and replays the failed request. Concurrent 401s queue behind a
single refresh — without that, a dashboard firing five requests would burn five
rotations and log the user out.

Storage: `mb_token`, `mb_refresh_token`, `mb_user` in `localStorage`.

---

## Patient

| Screen | Frontend call | Endpoint |
|---|---|---|
| Overview | `patientProfileService.stats` | GET `/patient/stats` |
| Settings | `patientProfileService.get` / `.update` | GET/PUT `/patient/profile` |
| Settings | `patientProfileService.changePassword` | POST `/patient/change-password` |
| Find Doctors | `doctorService.getDoctors` | GET `/doctors?specialization=&search=` |
| Book — step 1 | `doctorService.getSpecialties` | GET `/specialties` |
| Book — step 3 | `doctorService.getAvailableSlots` | GET `/doctors/{id}/slots?date=` |
| Checkout | `appointmentService.bookAppointment` | POST `/appointments` |
| Checkout | `paymentService.createOrder` → `.verify` | POST `/payments/order`, `/payments/verify` |
| Appointments | `fetchPatientAppointments` | GET `/appointments/patient` |
| Appointments | `appointmentService.cancelAppointment` | PATCH `/appointments/{id}/cancel` |
| Rate | `reviewService.submit` | POST `/reviews` |
| Records | `recordService.getRecords` / `.upload` | GET/POST `/records` |
| Records | `recordService.download` | GET `/records/{id}/download` |
| Records | `recordService.downloadMedicalHistory` | GET `/records/medical-history/pdf` |
| Prescriptions | `prescriptionService.downloadPdf` | GET `/prescriptions/{id}/pdf` |

Changing a password revokes every refresh token, so a session opened with the old
password cannot outlive it.

---

## Doctor

| Screen | Frontend call | Endpoint |
|---|---|---|
| Overview / Appointments | `fetchDoctorDashboard` | GET `/appointments/doctor/dashboard` |
| Appointments | `appointmentService.completeAppointment` | PATCH `/appointments/{id}/complete` |
| Appointments | `appointmentService.cancelAppointment` | PATCH `/appointments/{id}/cancel` |
| **Write Prescription** | `prescriptionService.create` | POST `/prescriptions` |
| Patient Records | `doctorProfileService.getPatients` | GET `/doctor/patients` |
| Patient Records → modal | *(direct)* | GET `/doctor/patients/{id}/records` |
| Manage Schedule | `doctorProfileService.getSchedule` / `.updateSchedule` | GET/PUT `/doctor/schedule` |
| Settings | `doctorProfileService.get` / `.update` | GET/PUT `/doctor/profile` |

The dashboard returns four buckets: `today`, `upcoming`, `pending`
(*past their time, awaiting notes*), `completed`.

There is **no accept/reject endpoint** — see the booking model below.

---

## Admin

| Screen | Frontend call | Endpoint |
|---|---|---|
| Overview | `adminService.getDashboard` | GET `/admin/dashboard` |
| Overview | `adminService.getActivity` | GET `/admin/activity?limit=` |
| Manage Patients | `adminService.getPatients` | GET `/admin/patients` |
| Manage Patients | `adminService.setPatientStatus` | PATCH `/admin/patients/{id}/status` |
| Manage Doctors | `adminService.getDoctors` | GET `/admin/doctors` |
| **Approve / Suspend** | `adminService.setDoctorStatus` | PATCH `/admin/doctors/{id}/status` |
| Appointments | `adminService.getAppointments` | GET `/admin/appointments` |
| Analytics | `adminService.getAnalytics` | GET `/admin/analytics` |
| System Settings | `settingsService.get` / `.update` | GET/PUT `/admin/settings` |
| Refunds | `adminService.refundPayment` | POST `/payments/{id}/refund` |

---

## The booking model — slot-based confirmation

Matches Practo / Apollo 24|7. A doctor who publishes a slot has already agreed to
be booked, so **paying for it confirms the appointment outright**.

```
patient picks slot
  → POST /appointments              status PENDING_PAYMENT
                                    fee + platform fee snapshotted onto the row
                                    slot held for `slot_hold_minutes` (default 15)
  → POST /payments/order            Razorpay order for the exact snapshotted total
  → Razorpay checkout               card details go to Razorpay, never to us
  → POST /payments/verify           HMAC-SHA256 signature checked server-side
                                    status ACCEPTED, meeting link generated + emailed
  → consultation happens
  → POST /prescriptions             status COMPLETED, patient notified
  → POST /reviews                   doctor's rating average recomputed
```

**If checkout is abandoned:** a job every 60s sets `AUTO_EXPIRED` and frees the
slot. Without it, closing the tab would block that time forever.

**If either side cancels:** refund is automatic — full for a doctor cancellation
or a patient cancelling outside the free window, partial inside it. All three
values live in `system_settings`, not in code.

---

## Money integrity

| Rule | Where enforced |
|---|---|
| Price fixed at booking | `booked_fee`, `platform_fee`, `total_amount` on `appointment` |
| Gateway charges the quoted total | `PaymentService.createOrder` reads `total_amount` |
| Amount never comes from the browser | server reads it from the appointment row |
| Payment only counts after verification | `POST /payments/verify` checks the HMAC |
| Failed attempts are recorded | `PaymentFailureRecorder` (`REQUIRES_NEW`, survives the rollback) |
| Refunds actually move money | `RazorpayGateway.refund` returns a gateway refund id |

Raising a doctor's fee after a booking does **not** change what that patient owes.

---

## Security

- **Ownership in the service layer.** `hasRole('PATIENT')` cannot express *their
  own* records, so every patient-scoped query is `findByIdAndPatientId`.
- **404, never 403, on ownership failure** — a 403 confirms the row exists.
- **Doctors see only patients they have treated**, derived from appointment
  history rather than the patient table.
- **Meeting links are time-boxed** to a window around the appointment.
- **Uploads** get a UUID filename and an allow-listed content type; the user's
  filename is never used as a path.
- **Login failures** say only "Invalid email or password".

---

## Running it

```bash
# backend  (needs application-local.yml — see the .example file)
cd medibridge-backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# frontend
cd medibridge-frontend
npm install && npm run dev
```

**Demo accounts** — patient `john.doe@email.com` · doctor
`sarah.johnson@medibridge.com` (both `Test@1234`) · admin `admin@medibridge.com`
(`Admin@123`).

**Razorpay test cards:** `4111 1111 1111 1111`, any future expiry / CVV.
UPI: `success@razorpay`.

`VITE_USE_MOCK=true` runs the frontend standalone with no backend at all.
