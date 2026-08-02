# CLAUDE.md

Two packages: `medibridge-frontend/` (React + Vite, no TypeScript) and
`medibridge-backend/` (Spring Boot 4.1 + MySQL). The backend was built to fit the
already-finished frontend, not the other way round.

## Commands

The `local` profile is required on every backend command — it supplies the DB
password and JWT secret from the gitignored `application-local.yml`. Without it
the app starts against an empty password and fails.

Backend runs on 8080 with context path `/api`; Vite on 5173.
Demo admin: `admin@medibridge.com` / `Admin@123` (seeded by `DataSeeder`).

There is no linter, no frontend test runner, no TypeScript. Do not invent
`npm test` / `npm run lint` — they don't exist.

## Backend architecture

**Modular monolith**: one deployable, feature packages under `com.medibridge`.
The rule that keeps it modular: **a module may import another module's `Service`
and `dto`, never its `entity` or `repository`.** `auth` is the one exception —
authentication inherently spans all three identity tables.

Three identity tables (`patient`, `doctor`, `admin`), each with its own
`password_hash`. Login sends `role` to pick the table. **That role is never
trusted as authority** — it selects where to look up the account; authority
comes from the row actually loaded.

## Things that will bite you

- **Spring Boot 4 split autoconfiguration into per-technology modules.**
  `flyway-core` alone gives the library with *no* Spring integration — migrations
  silently never run. You need `org.springframework.boot:spring-boot-flyway`.
  Expect the same for other integrations (see `docs/testing.md` for the test one).
- **`ddl-auto: validate` and `baseline-on-migrate: false` are deliberate.**
  Flyway owns the schema; validate catches entity/schema drift at startup.
  `baseline-on-migrate: true` would make Flyway silently skip V1.
- **Java enums vs MySQL ENUM literals need converters.** `@Enumerated(STRING)`
  writes `ACTIVE`; the column holds `'active'`. See `common/enums/converter/`.
  A new enum-backed column means a new converter.
- **JSON casing is mixed and must stay that way.** Entity fields snake_case,
  admin aggregates camelCase. No global Jackson strategy — explicit
  `@JsonProperty` on DTOs.
- **`AppointmentStatus.toFrontend()` exists because the DB and UI disagree.**
  DB has `Requested`/`Accepted`/`Completed`; `Badge.jsx` only colours
  `pending`/`confirmed`/`cancelled`. Never return the raw DB value to a client.
- **JWT tokens are HS384, not HS256.** `JwtService`'s javadoc says HS256 and is
  wrong — no explicit algorithm is passed, so JJWT picks the strongest the
  62-byte key supports. Anything verifying these outside Spring must not
  hardcode HS256.
- **PDF templates must be well-formed XHTML** — openhtmltopdf throws on unclosed
  tags, and built-in Helvetica has no glyph for ℞ (U+211E).

## Security invariants — do not weaken these

- **Ownership checks live in the service layer, not in URL rules.**
  `hasRole('PATIENT')` cannot express "only *their own* records". Every
  patient-scoped lookup queries by `(id, ownerId)` — see `findByIdAndPatientId`.
  Take the caller's id from `@CurrentUser`, never from a request parameter.
- **Return 404, not 403, on ownership failures.** A 403 confirms it exists.
- Login failures say "Invalid email or password" regardless of cause — anything
  more specific enumerates registered accounts.
- Double booking is prevented by the UNIQUE constraint on
  `appointment.schedule_id`. `doctor_schedule.is_booked` is a read cache only —
  never the booking guard.
- Uploaded files get a generated UUID filename and an allow-listed content type;
  the user's filename is never used as a path.

## Mock-vs-live API architecture

The central design constraint. The frontend runs standalone against mock data or
against the real backend, switched by one env flag:

```
VITE_USE_MOCK=true     # false -> real HTTP calls
```

`USE_MOCK` is exported from `src/api/axiosClient.js` and defaults to **true**
unless the string is exactly `"false"`. Every method in `src/services/*` branches
on it — `if (USE_MOCK) return mockResolve(fixture)` then the axios call.
**Components never branch on mock mode.** Adding a feature means the service
method with *both* branches plus a fixture in `src/api/mock/mockData.js`, or the
app breaks in its default configuration.

## Frontend conventions

`page` → `dispatch(thunk)` → `features/*/slice.js` → `services/*Service.js` →
mock or `axiosClient`. Slices expose `status`/`error`
(`'idle' | 'loading' | 'succeeded' | 'failed'`).

- Mock fixtures mirror the MySQL schema — entity fields are **snake_case**
  (`appointment_id`, `full_name`). Only the auth `user` object is camel-ish.
- `authSlice` is the only persisted slice: `mb_token` / `mb_user` /
  `mb_refresh_token` in `localStorage`, rehydrated into `initialState`.
- Tailwind only. `Badge.jsx` maps status → colour and is **case-sensitive**;
  a new status needs an entry there or it falls back to blue.
- Reuse `src/components/common/` primitives rather than re-styling ad hoc.
- Adding a dashboard page means a route **and** a nav entry in
  `patientNav.js` / `doctorNav.js` / `adminNav.js`.
- Some dashboard numbers are hardcoded to match the wireframes. Check before
  assuming a value is wired up.
- `src/api/mock/mockData.js` holds already-mis-encoded emoji — targeted edits
  only, never a whole-file rewrite.

## Tests

See `docs/testing.md`. Integration tests against a real MySQL schema; do not
run them unless I say so.

## Working style

This section is loaded into context on **every** message, so anything added here
is paid for every time. Only rules that change behaviour belong in it.

**Verifying**

- Do not run anything unless I say "run it". No dev server, no browser,
  no preview, no screenshots, no `mvn test`, no `npm run build`.
- `mvn -q compile -o` is the one exception: prints nothing on success,
  so it costs no tokens. Use it to check a change compiles.
- I run the dev server in my own terminal and verify all visual changes myself.
- When I do say "run it": one filtered command, report the last line only.
  Never paste full logs, stack traces, or test output back to me.
- Don't restart servers, re-seed the database, or re-run migrations.

**UI work**

- Write the CSS/Tailwind once and stop. No iterate-and-recheck loops,
  no "let me adjust the spacing" follow-ups.
- Report what you changed in 3-5 lines. I'll tell you if it's wrong.

**Reading the codebase**

- Grep before Read. Read line ranges, not whole files, for anything long.
- Don't re-read a file already read this session, and don't re-derive anything
  already stated in this file.
- Batch independent tool calls into one message.

**Answering**

- Lead with the answer. No alternatives unless asked.
- Plans: output only, no prose explanation.
- After a change, 3-5 lines: what changed, anything surprising.
  No walkthrough of code I can read myself.
- Ask before starting work when the request is ambiguous. A wrong guess costs
  more than the question.
