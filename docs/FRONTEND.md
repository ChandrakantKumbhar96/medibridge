# MediBridge — Frontend Documentation

React single-page application for the MediBridge telemedicine platform.

---

## 1. Technology stack

| Concern | Technology |
|---|---|
| Library | React 18 |
| Build tool | Vite 5 |
| State management | Redux Toolkit + React Redux |
| Routing | React Router 6 |
| HTTP | Axios |
| Styling | Tailwind CSS 3 |
| Icons | lucide-react |
| Payments | Razorpay Checkout (hosted) |
| Auth (social) | Google Identity Services |

---

## 2. Project structure

```
src/
├── main.jsx                  # React entry point
├── App.jsx                   # <BrowserRouter> + <AppRoutes>
│
├── api/
│   ├── axiosClient.js        # Axios instance: JWT header + 401 refresh interceptor
│   └── mock/mockData.js      # fixtures for standalone (mock) mode
│
├── app/
│   └── store.js              # Redux store (5 slices)
│
├── features/                 # Redux Toolkit slices (createAsyncThunk)
│   ├── auth/authSlice.js
│   ├── doctors/doctorsSlice.js
│   ├── appointments/appointmentsSlice.js
│   ├── records/recordsSlice.js
│   └── admin/adminSlice.js
│
├── services/                 # API layer — one file per domain (12 files)
│   ├── authService.js        payoutService.js
│   ├── doctorService.js      prescriptionService.js
│   ├── appointmentService.js profileService.js
│   ├── paymentService.js     recordService.js
│   ├── adminService.js       reviewService.js
│   ├── razorpay.js           _mock.js
│
├── components/
│   ├── common/               # Button, Card, Input, Badge, Avatar, Logo, StatCard,
│   │                         #   GoogleSignInButton, RescheduleModal
│   ├── layout/               # DashboardLayout, Sidebar, DashboardTopbar, PublicNavbar
│   └── routing/              # ProtectedRoute (RBAC guard)
│
├── pages/                    # 27 screens
│   ├── public/               # Landing, Login, AdminLogin, register forms
│   ├── patient/              # Overview, Appointments, FindDoctors, Book, Payment,
│   │                         #   Records, Settings, RateExperience
│   ├── doctor/               # Overview, Appointments, PatientRecords, Schedule,
│   │                         #   WritePrescription, Earnings, Settings
│   └── admin/                # Overview, ManagePatients, ManageDoctors, Appointments,
│                             #   Payouts, Analytics, SystemSettings
│
├── routes/AppRoutes.jsx      # all routes + role protection
└── utils/exportCsv.js        # client-side CSV export
```

---

## 3. Data-flow architecture

Every screen follows the same path — this is the single most important thing to
understand about the frontend:

```
page component
  → dispatch(thunk)              features/*/slice.js  (createAsyncThunk)
  → service method              services/*Service.js  (the ONLY place URLs live)
  → axiosClient                 api/axiosClient.js
  → HTTP → Spring Boot API
```

**Two rules that hold it together:**

1. **Components never call `axios` directly** — only `services/*`. This is what
   makes the mock/live switch a single flag.
2. **Pages read state with `useSelector` and fire fetch thunks from a `useEffect`
   on mount.** Slices expose `status` (`idle | loading | succeeded | failed`).

---

## 4. State management (Redux Toolkit)

Five slices in `store.js`:

| Slice | Holds | Persisted? |
|---|---|---|
| `auth` | current user, token, isAuthenticated | ✅ localStorage |
| `doctors` | doctor list, specialties | no |
| `appointments` | patient upcoming/past, doctor dashboard buckets | no |
| `records` | medical records list | no |
| `admin` | dashboard stats, patients, doctors, analytics | no |

Only `auth` persists — it writes `mb_token`, `mb_refresh_token`, `mb_user` to
`localStorage` and rehydrates `initialState` from them on load.

---

## 5. The service layer (mock vs live)

The central design constraint: the app can run **standalone against mock data**,
then switch to the real backend by flipping one env flag. Every service method
has both branches:

```js
async getPatientAppointments() {
  if (USE_MOCK) return mockResolve(patientAppointments)   // mock fixture
  const { data } = await axiosClient.get('/appointments/patient')
  return data
}
```

`USE_MOCK` is exported from `axiosClient.js` and is **true unless
`VITE_USE_MOCK` is exactly `"false"`**.

**The 12 service files** map one-to-one to backend domains: auth, doctor,
appointment, payment, admin, payout, prescription, profile, record, review, plus
`razorpay.js` (checkout helper) and `_mock.js` (latency simulator).

---

## 6. Authentication & the 401 refresh interceptor

`axiosClient.js` does two things:

1. **Request interceptor** attaches `Authorization: Bearer <mb_token>` to every call.
2. **Response interceptor** — on a `401`, it silently exchanges the refresh token
   for a new pair and **replays the failed request once**. Concurrent 401s queue
   behind a single refresh so they don't each burn a rotation. If the refresh
   token is also dead, it clears storage and redirects to `/login`.

This is why a user is never abruptly logged out when the 15-minute access token
expires mid-session.

---

## 7. Routing & role protection

Three route trees in `AppRoutes.jsx`, each wrapped in `ProtectedRoute`:

```
/                     public landing
/login                patient + doctor (role toggle)
/admin/login          admin

/patient/*            ProtectedRoute role="patient"
/doctor/*             ProtectedRoute role="doctor"
/admin/*              ProtectedRoute role="admin"
```

`ProtectedRoute` guards on `isAuthenticated` + exact `user.role`; a role mismatch
redirects to `/login`. Each dashboard tree uses `<DashboardLayout navItems={...}>`
with a nav array (`patientNav.js` / `doctorNav.js` / `adminNav.js`).

---

## 8. UI conventions

- **Tailwind only** — no CSS modules or styled-components. Custom `primary.*`
  blue scale in `tailwind.config.js`; slate is the neutral.
- **`Badge.jsx`** maps status strings to colours (case-sensitive).
- **Shared primitives** in `components/common/` — reuse Button, Card, Input, etc.
  rather than re-styling ad hoc.
- **Currency is ₹ (INR)** throughout; numbers formatted with
  `toLocaleString('en-IN')`.
- Adding a dashboard page means adding a **route** *and* a **nav entry**.

---

## 9. Configuration & running

### `.env` (create from `.env.example`)

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=false          # false → real backend; true → mock data, no backend needed
VITE_GOOGLE_CLIENT_ID=<your Google OAuth web client id>
```

> Vite reads `.env` only at startup — restart `npm run dev` after editing it.

### Commands

```bash
npm install       # first time only
npm run dev       # dev server → http://localhost:5173
npm run build     # production build → dist/
npm run preview   # preview the production build
```

### Run order

Start the **backend first** (port 8080), then the frontend (port 5173) — the
frontend calls the API on page load. `VITE_USE_MOCK=true` is the exception: the
app runs fully standalone with no backend.

---

## 10. Screen inventory (27 pages)

**Public (5)** — Landing, Login, Admin Login, Patient Register, Doctor Register

**Patient (8)** — Overview, Appointments, Find Doctors, Book Appointment (wizard),
Payment/Checkout, Medical Records, Settings, Rate Experience

**Doctor (7)** — Overview, Appointments, Patient Records, Manage Schedule, Write
Prescription, Earnings, Settings

**Admin (7)** — Overview, Manage Patients, Manage Doctors, Appointments, Doctor
Payouts, Analytics, System Settings

See the endpoint-to-screen mapping in [../CONNECTIVITY.md](../CONNECTIVITY.md).
