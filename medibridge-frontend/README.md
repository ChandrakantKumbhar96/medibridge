# MediBridge — Frontend

Digital healthcare platform frontend built exactly to the provided wireframes.
Stack: **React + Vite + Redux Toolkit + React Router + Tailwind CSS + Axios**.

Three portals with role-based routing: **Patient**, **Doctor**, **Admin**.

## Quick start

```bash
npm install
npm run dev
```

Open http://localhost:5173

### Login (mock mode is ON by default)
Any email/password works. Pick the role on the login screen:
- **Patient** tab → patient dashboard
- **Doctor** tab → doctor dashboard
- **Admin Login →** link (or `/admin/login`) → admin dashboard

## Connecting your Spring Boot backend

The app ships with realistic mock data so it runs with zero backend.
When your backend is ready, edit `.env`:

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=false     # switch mock off to use live endpoints
```

Every API call already lives in `src/services/*`. Each method calls the real
endpoint when `VITE_USE_MOCK=false`, and returns mock data otherwise — so you
only flip one flag, no component changes needed. A JWT stored in `localStorage`
is auto-attached as `Authorization: Bearer <token>` by `src/api/axiosClient.js`.

### Expected endpoints (align your Spring Boot controllers)
```
POST /auth/login                 { email, password, role }
POST /auth/register/patient
POST /auth/register/doctor
POST /auth/otp/request           { phone }   -- patients only; same reply either way
POST /auth/otp/verify            { phone, code }   -- unknown number auto-registers
GET  /doctors
GET  /specialties
GET  /appointments/patient
POST /appointments               { doctor_id, schedule_id, consult_type, reason,
                                   family_member_id? }   -- null = for myself
POST /appointments/{id}/follow-up { schedule_id }   -- free revisit, same doctor
PATCH /appointments/{id}/cancel
GET  /appointments/doctor/dashboard
GET  /records                    ?subject=self | <family_member_id> | (omitted = all)
POST /records                    ?report_name&report_type&family_member_id?
GET  /patient/family
POST /patient/family             { full_name, date_of_birth, gender, relation, ... }
PUT  /patient/family/{id}
DELETE /patient/family/{id}      -- archives; history stays readable
GET  /admin/dashboard | /admin/patients | /admin/doctors
GET  /admin/appointments | /admin/analytics | /admin/settings
```

## Project structure

```
src/
├── api/            axios client + mock data (mirrors the MySQL schema)
├── app/            Redux store
├── features/       Redux Toolkit slices (auth, doctors, appointments, records, admin)
├── services/       API service layer with mock fallback per method
├── components/
│   ├── common/     Button, Badge, Card, Input, Avatar, StatCard, Logo
│   ├── layout/     PublicNavbar, DashboardTopbar, Sidebar, DashboardLayout
│   └── routing/    ProtectedRoute (RBAC guard)
├── pages/
│   ├── public/     Landing, Login (patient/doctor tabs + register), Admin login
│   ├── patient/    Overview, Appointments, Find Doctors, Book wizard, Records,
│   │               Settings, Payment, Rate Experience
│   ├── doctor/     Overview, Appointments, Patient Records, Schedule, Settings
│   └── admin/      Overview, Manage Patients, Manage Doctors, Appointments,
│                   Analytics, System Settings
└── routes/         AppRoutes (all routes + role protection)
```

## Pages ↔ wireframes
All 22 wireframe screens are implemented, plus the booking flow, payment, and
rating screens shown in the flow diagram.

## Build
```bash
npm run build      # outputs to dist/
npm run preview
```
