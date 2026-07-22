# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Two packages: `medibridge-frontend/` (React + Vite) and `medibridge-backend/`
(Spring Boot 4.1 + MySQL). They are developed together — the backend was built
to fit the already-finished frontend, not the other way round.

## Commands

Frontend, from `medibridge-frontend/`:

```bash
npm install
npm run dev        # Vite dev server, port 5173, opens browser
npm run build      # -> dist/
```

Backend, from `medibridge-backend/`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local   # port 8080, context path /api
mvn compile
```

The `local` profile is required — it supplies the DB password and JWT secret
from the gitignored `src/main/resources/application-local.yml`. Without it the
app starts against an empty password and fails.

Demo admin: `admin@medibridge.com` / `Admin@123` (seeded by `DataSeeder`).

## Backend architecture

**Modular monolith**, not microservices: one deployable, organized into feature
packages under `com.medibridge` — `auth`, `patient`, `doctor`, `appointment`,
`record`, `prescription`, `payment`, `review`, `admin`, `notification`, `pdf`,
plus shared `common/`.

The rule that keeps it modular: **a module may import another module's `Service`
and `dto`, never its `entity` or `repository`.** `auth` is the one documented
exception — authentication inherently spans all three identity tables.

Three separate identity tables (`patient`, `doctor`, `admin`), each with its own
`password_hash`. Login sends `role` to pick the table. **That role is never
trusted as authority** — it selects where to look up the account; authority
comes from the row actually loaded.

## Things that will bite you

- **Spring Boot 4 split its autoconfiguration into per-technology modules.**
  `flyway-core` alone gives you the library with *no* Spring integration —
  migrations silently never run, then Hibernate fails with a confusing
  "missing table". You need `org.springframework.boot:spring-boot-flyway`.
  Expect the same pattern for other integrations.

- **`ddl-auto: validate` and `baseline-on-migrate: false` are deliberate.**
  Flyway owns the schema; validate catches entity/schema drift at startup
  (it already caught `Short` → SMALLINT vs a TINYINT column). `baseline-on-migrate:
  true` would make Flyway silently skip V1 against a non-empty database.

- **Java enums vs MySQL ENUM literals need converters.** `@Enumerated(STRING)`
  writes the Java constant name (`ACTIVE`, `BEDSIDE_MANNER`), but the columns
  hold `'active'` and `'Bedside Manner'`. See `common/enums/converter/`. Adding a
  new enum-backed column means adding a converter.

- **JSON casing is mixed and must stay that way.** Entity fields are snake_case
  (`appointment_id`, `full_name`); admin aggregates are camelCase
  (`totalPatients`, `revenueMTD`). A global Jackson naming strategy breaks one or
  the other — use explicit `@JsonProperty` on DTOs.

- **`AppointmentStatus.toFrontend()` exists because the DB and the UI disagree.**
  The DB has `Requested`/`Accepted`/`Completed`; `Badge.jsx` only colours
  `pending`/`confirmed`/`cancelled`. Never return the raw DB value to the client.

- **PDF templates must be well-formed XHTML** — openhtmltopdf throws on unclosed
  tags. It also uses built-in Helvetica, which has no glyph for symbols like
  U+211E (℞ renders as a box). Use plain text or embed a font.

## Security invariants — do not weaken these

- **Ownership checks live in the service layer, not in URL rules.**
  `hasRole('PATIENT')` cannot express "only *their own* records". Every
  patient-scoped lookup queries by `(id, ownerId)` — see
  `findByIdAndPatientId`. Take the caller's id from `@CurrentUser`, never from a
  request parameter.
- **Return 404, not 403, on ownership failures.** A 403 confirms the resource
  exists.
- Login failures say "Invalid email or password" regardless of cause — anything
  more specific enumerates registered accounts.
- Double booking is prevented by the UNIQUE constraint on
  `appointment.schedule_id`. `doctor_schedule.is_booked` is a read cache only —
  never use it as the booking guard.
- Uploaded files get a generated UUID filename and an allow-listed content type;
  the user's filename is never used as a path.

There is no linter, test runner, or TypeScript in this project. Do not invent `npm test` / `npm run lint` — they don't exist. Verification is done by running the dev server.

## Mock-vs-live API architecture

This is the central design constraint of the codebase. The frontend was built to run standalone against mock data, then switch to a Spring Boot backend by flipping one env flag in `.env`:

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=true     # false -> real HTTP calls
```

`USE_MOCK` is exported from [axiosClient.js](medibridge-frontend/src/api/axiosClient.js) and defaults to **true** unless the string is exactly `"false"`.

Every method in `src/services/*` follows this shape and **must keep following it** — components never branch on mock mode:

```js
async getPatientAppointments() {
  if (USE_MOCK) return mockResolve(patientAppointments)   // from src/services/_mock.js
  const { data } = await axiosClient.get('/appointments/patient')
  return data
}
```

`mockResolve` deep-clones and resolves after ~300ms to emulate latency. When adding a feature, add the service method with both branches plus a fixture in `src/api/mock/mockData.js` — otherwise the app breaks in its default configuration.

The backend contract (endpoints the Spring Boot side is expected to expose) is listed in [README.md](medibridge-frontend/README.md).

## Data flow

`page component` → `dispatch(thunk)` → `features/*/slice.js` (createAsyncThunk) → `services/*Service.js` → mock or `axiosClient`.

- Store: [store.js](medibridge-frontend/src/app/store.js) — slices `auth`, `doctors`, `appointments`, `records`, `admin`.
- Pages read state with `useSelector` and fire fetch thunks from a `useEffect` on mount. Slices expose `status`/`error` strings (`'idle' | 'loading' | 'succeeded' | 'failed'`).
- Mock fixtures mirror the MySQL schema, so entity fields are **snake_case** (`appointment_id`, `full_name`, `report_id`, `consultation_fee`). Keep that convention in new fixtures and payloads; only the auth `user` object is camel-ish (`{ id, name, email, role }`).

## Auth and RBAC

- [authSlice.js](medibridge-frontend/src/features/auth/authSlice.js) is the only slice with persistence: it writes `mb_token` / `mb_user` to `localStorage` and rehydrates `initialState` from them on load.
- `axiosClient` request interceptor reads `mb_token` directly from `localStorage` and sets `Authorization: Bearer <token>`.
- [ProtectedRoute.jsx](medibridge-frontend/src/components/routing/ProtectedRoute.jsx) guards on `isAuthenticated` + exact `user.role`; a role mismatch redirects to `/login`, not to the user's own dashboard.
- Three roles → three route trees in [AppRoutes.jsx](medibridge-frontend/src/routes/AppRoutes.jsx): `/patient/*`, `/doctor/*`, `/admin/*`. Patient and doctor share `/login` (role toggle); admin has a separate `/admin/login`.
- In mock mode any email/password works; the role picked on the login screen determines the mock user returned.

## UI conventions

- Dashboard pages render `<DashboardLayout navItems={...}>`; the nav arrays live next to the pages as `patientNav.js` / `doctorNav.js` / `adminNav.js` (lucide-react icons). Adding a dashboard page means adding a route **and** a nav entry.
- Tailwind only — no CSS modules or styled-components. Custom `primary.*` blue scale in [tailwind.config.js](medibridge-frontend/tailwind.config.js); slate is the neutral.
- [Badge.jsx](medibridge-frontend/src/components/common/Badge.jsx) maps status strings to colors and is **case-sensitive** with mixed conventions already present (`confirmed`, `Requested`, `Cancelled`). New statuses need an entry in that map or they fall back to blue.
- Shared primitives in `src/components/common/` (Button, Card, Input + `Field`, Avatar, StatCard, Logo) — reuse rather than re-styling ad hoc.

## Gotchas

- The layout matches a fixed set of wireframes; some dashboard numbers (e.g. the stat tiles in `PatientOverview`) are hardcoded rather than derived from store data. Check before assuming a value is wired up.
- `src/api/mock/mockData.js` contains emoji that are already stored mis-encoded. When editing it, use targeted edits — rewriting the whole file risks mangling those strings further.
