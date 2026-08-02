# MediBridge — Database Documentation

MySQL 8 schema, owned and versioned by **Flyway** (migrations V1–V15).
26 tables. Character set `utf8mb4`, engine InnoDB.

---

## 1. Migration history

| Version | Adds |
|---|---|
| **V1** | Core schema — identity, doctors, appointments, records, prescriptions, payments, reviews, settings, audit |
| **V2** | Reference data — specializations, system settings |
| **V3** | Google Sign-In columns on `patient`; payment-gateway columns |
| **V4** | Money integrity (fee snapshots), slot holds, meeting windows, `notification` table, CHECK constraints |
| **V5** | Doctor earnings ledger, payout batches, reschedule tracking |
| **V6** | WhatsApp added as a notification channel |
| **V7** | Doctor `qualifications` and `languages` |
| **V8** | No-show outcome and join-timestamp tracking |
| **V9** | `drug` / `drug_alias` / `drug_interaction` reference data |
| **V10** | Live-queue delay thresholds (settings only, no schema change) |
| **V11** | `medical_opinion` — second opinion as its own document |
| **V12** | `family_member` — dependent profiles |
| **V13** | Free follow-up window (`parent_appointment_id` self-reference) |
| **V14** | Phone OTP login (`phone_e164`, `phone_otp`, email made optional) |
| **V15** | "Next available" matching thresholds (settings only, no schema change) |

`ddl-auto: validate` runs at startup — Hibernate confirms every entity matches
its table, catching drift before it becomes corrupt data. `baseline-on-migrate`
is deliberately `false`: Flyway owns V1 onward, nothing is allowed to skip it.

---

## 2. Entity-relationship diagram

The core clinical, booking and money domain — every table connected by a real
foreign key. Five infrastructure tables (`system_settings`, `notification`,
`activity_log`, `refresh_token`, `phone_otp`) are polymorphic or standalone by
design and are covered in §4 instead of cluttering this diagram.

```mermaid
erDiagram
    SPECIALIZATION ||--o{ DOCTOR : classifies
    DOCTOR ||--o{ DOCTOR_AVAILABILITY : "weekly template"
    DOCTOR ||--o{ DOCTOR_SCHEDULE : "publishes slots"
    DOCTOR ||--o{ APPOINTMENT : "is booked for"
    DOCTOR ||--o{ DOCTOR_EARNING : earns
    DOCTOR ||--o{ DOCTOR_PAYOUT : "is settled in"
    DOCTOR ||--o{ PRESCRIPTION : writes
    DOCTOR ||--o{ RATING : "is rated on"

    PATIENT ||--o{ APPOINTMENT : books
    PATIENT ||--o{ FAMILY_MEMBER : owns
    PATIENT ||--o{ MEDICAL_REPORT : uploads
    PATIENT ||--o{ PRESCRIPTION : receives
    PATIENT ||--o{ RATING : writes

    FAMILY_MEMBER |o--o{ APPOINTMENT : "is subject of"
    FAMILY_MEMBER |o--o{ MEDICAL_REPORT : "is subject of"

    DOCTOR_SCHEDULE ||--o| APPOINTMENT : reserves

    APPOINTMENT |o--o| CONSULTATION_RECORD : produces
    APPOINTMENT |o--o| MEDICAL_OPINION : produces
    APPOINTMENT |o--o| RATING : receives
    APPOINTMENT ||--o{ PAYMENT_TRANSACTION : generates
    APPOINTMENT |o--o| DOCTOR_EARNING : credits
    APPOINTMENT |o--o| APPOINTMENT : "follow-up of"

    CONSULTATION_RECORD |o--o| PRESCRIPTION : yields
    PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
    PRESCRIPTION_ITEM }o--o| DRUG : identifies

    DRUG ||--o{ DRUG_ALIAS : "aka"
    DRUG ||--o{ DRUG_INTERACTION : "pairs with"

    RATING ||--o{ RATING_HIGHLIGHT : tags

    DOCTOR_PAYOUT ||--o{ DOCTOR_EARNING : settles

    SPECIALIZATION {
        int specialization_id PK
        string name UK
        string emoji
        string description
    }

    DOCTOR {
        char36 doctor_id PK "UUID"
        string full_name
        string email UK
        string password_hash
        string phone
        int specialization_id FK
        string license_number UK
        int experience_years
        decimal consultation_fee
        int consultation_duration_min
        decimal rating_avg
        int rating_count
        string qualifications
        string languages
        enum status "pending/active/inactive/suspended"
    }

    PATIENT {
        int patient_id PK
        string full_name
        string email UK "nullable - phone-first signup"
        string password_hash "nullable - OAuth/OTP accounts"
        enum auth_provider "LOCAL/GOOGLE/PHONE"
        string google_sub UK
        string phone
        string phone_e164 UK "normalised E.164 login key"
        date date_of_birth "nullable until profile complete"
        string gender
        string blood_group
        enum status "active/inactive"
    }

    FAMILY_MEMBER {
        int family_member_id PK
        int patient_id FK
        string full_name
        date date_of_birth
        enum gender
        enum relation "Child/Spouse/Parent/Sibling/Other"
        datetime archived_at "soft delete"
    }

    DOCTOR_AVAILABILITY {
        int availability_id PK
        char36 doctor_id FK
        enum day_of_week
        boolean morning "09:00-12:00"
        boolean afternoon "14:00-17:00"
    }

    DOCTOR_SCHEDULE {
        int schedule_id PK
        char36 doctor_id FK
        date available_date
        time start_time
        time end_time
        boolean is_booked "read cache only - NOT the booking guard"
    }

    APPOINTMENT {
        int appointment_id PK
        int patient_id FK
        int family_member_id FK "null = the account holder themself"
        char36 doctor_id FK
        int schedule_id FK UK "UNIQUE = the real double-booking guard"
        int parent_appointment_id FK UK "free follow-up source, UNIQUE = one per parent"
        datetime appointment_date
        enum status "PendingPayment/Accepted/Completed/Cancelled/AutoExpired/NoShow/..."
        string consult_type "Consultation/SecondOpinion"
        decimal booked_fee "snapshotted at booking"
        decimal platform_fee "snapshotted at booking"
        decimal total_amount
        datetime hold_expires_at
        string meeting_link
        datetime meeting_join_from
        datetime meeting_valid_until
        datetime patient_joined_at
        datetime doctor_joined_at
        enum no_show_by "PATIENT/DOCTOR/BOTH"
        tinyint reschedule_count
        enum cancelled_by "PATIENT/DOCTOR/ADMIN/SYSTEM"
    }

    MEDICAL_REPORT {
        int report_id PK
        int patient_id FK
        int family_member_id FK "whose document this is"
        string report_name
        string report_type
        string content_type
        enum uploaded_by_type "PATIENT/DOCTOR/SYSTEM"
    }

    CONSULTATION_RECORD {
        int consultation_id PK
        int appointment_id FK UK
        string diagnosis
        text notes
        date follow_up_date
    }

    PRESCRIPTION {
        int prescription_id PK
        int consultation_id FK UK
        int patient_id FK
        char36 doctor_id FK
        date date_issued
        text advice
    }

    PRESCRIPTION_ITEM {
        int item_id PK
        int prescription_id FK
        int drug_id FK "nullable - free text not in reference data"
        string medicine_name
        string dosage
        string frequency
        string duration "display string, e.g. '5 days'"
        int duration_days "computable course end"
    }

    DRUG {
        int drug_id PK
        string name UK
        string generic_name "what interactions really check"
        string drug_class
    }

    DRUG_ALIAS {
        int alias_id PK
        int drug_id FK
        string alias UK "brand name / common spelling"
    }

    DRUG_INTERACTION {
        int interaction_id PK
        int drug_a_id FK "drug_a_id < drug_b_id enforced"
        int drug_b_id FK
        enum severity "Minor/Moderate/Severe"
        string description
    }

    MEDICAL_OPINION {
        int opinion_id PK
        int appointment_id FK UK "one appointment, one opinion"
        text original_diagnosis
        text findings
        boolean agrees_with_original
        text recommendation
        text suggested_tests
    }

    RATING {
        int rating_id PK
        int appointment_id FK UK "one review per visit"
        int patient_id FK
        char36 doctor_id FK
        tinyint stars "CHECK 1-5"
        enum overall_experience
        text review_text
    }

    RATING_HIGHLIGHT {
        int rating_id PK_FK
        enum highlight PK "multi-select tag"
    }

    PAYMENT_TRANSACTION {
        int transaction_id PK
        int appointment_id FK
        decimal amount
        decimal consultation_amount
        decimal platform_fee
        enum gateway "SIMULATED/RAZORPAY"
        string gateway_order_id UK
        string gateway_payment_id
        string gateway_signature
        enum transaction_status "Pending/Paid/Refunded/Failed"
        decimal refund_amount
        string gateway_refund_id
    }

    DOCTOR_EARNING {
        int earning_id PK
        char36 doctor_id FK
        int appointment_id FK UK "one earning per visit"
        decimal gross_amount "consultation fee only"
        decimal commission_rate "snapshotted"
        decimal commission_amount
        decimal net_amount
        enum status "PENDING/SETTLED/REVERSED"
        int payout_id FK
    }

    DOCTOR_PAYOUT {
        int payout_id PK
        char36 doctor_id FK
        date period_start
        date period_end
        int consultations
        decimal gross_amount
        decimal commission
        decimal net_amount
        enum status "PENDING/PAID/FAILED"
        string payout_ref
    }
```

Read `||--o{` as "exactly one, to zero-or-many" and `|o--o{` / `|o--o|` as
"zero-or-one, to zero-or-many/one" — most of the optional sides above are
optional because the row doesn't exist yet (no rating until the visit
happens, no consultation record until the doctor writes one), not because the
relationship is genuinely many-valued in the other direction.

---

## 3. Tables by domain

### Identity (3 separate tables, deliberately not unified)

| Table | Purpose | Notable columns |
|---|---|---|
| `patient` | patient accounts | `auth_provider` (LOCAL/GOOGLE/PHONE), `google_sub`, `phone_e164`, nullable `email`/`password_hash`/`date_of_birth` for accounts that haven't completed onboarding |
| `doctor` | doctor accounts + public profile | `id` (UUID), `specialization_id` (FK), `license_number`, `consultation_fee`, `rating_avg`, `qualifications`, `languages`, `status` |
| `admin` | admin accounts | email + password only, no self-registration path |
| `refresh_token` | hashed, revocable session tokens | `token_hash`, `user_type` + `user_id` (polymorphic), `expires_at`, `revoked` |
| `phone_otp` | one-time login codes | `phone_e164`, `code_hash`, `expires_at`, `attempts`, `consumed_at` — no FK to `patient`, because the account may not exist yet |

Each identity table has its own `password_hash`; there is no shared `users`
table. `doctor.id` is a UUID rather than an int specifically so public doctor
listings can't be enumerated by incrementing an id. See
[BUSINESS_LOGIC.md §1](BUSINESS_LOGIC.md#1-identity--authentication) for how
login picks the right table without trusting the caller's claimed role.

### Doctors & scheduling

| Table | Purpose |
|---|---|
| `specialization` | lookup: name, emoji, description — fixes the free-text mismatch between the register dropdown and doctor listing pages |
| `doctor_availability` | the recurring **weekly template** the Manage Schedule screen edits (day + morning/afternoon) |
| `doctor_schedule` | **concrete, dated** bookable slots generated from the template, sliced by `consultation_duration_min`; `UNIQUE(doctor, date, start_time)` |

### Appointments & dependents

| Table | Key columns |
|---|---|
| `appointment` | the centre of the schema — see [BUSINESS_LOGIC.md §3](BUSINESS_LOGIC.md#3-the-booking-lifecycle) for the full lifecycle |
| `family_member` | dependents a patient books/uploads on behalf of; soft-deleted (`archived_at`), never hard-deleted while clinical history references them |

`UNIQUE(schedule_id)` on `appointment` is the real double-booking guard —
`doctor_schedule.is_booked` is only ever a read cache. `UNIQUE(parent_appointment_id)`
caps a completed visit at exactly one free follow-up.

### Clinical records

| Table | Purpose |
|---|---|
| `consultation_record` | one per appointment: diagnosis, notes, follow-up date |
| `prescription` | one per consultation: patient, doctor, date issued, advice |
| `prescription_item` | medicines: name, dosage, frequency, duration, optional `drug_id` link |
| `drug` / `drug_alias` / `drug_interaction` | canonical medicine identity, brand-name aliases, and a small interaction-checking dataset — see [BUSINESS_LOGIC.md §11](BUSINESS_LOGIC.md#11-drug-identity--interaction-checking) |
| `medical_opinion` | the second-opinion verdict document — deliberately not a prescription variant |
| `medical_report` | uploaded documents: name, type, file path, uploader, optional dependent subject |

### Payments & payouts

| Table | Key columns |
|---|---|
| `payment_transaction` | `appointment_id`, split `consultation_amount`/`platform_fee`, `gateway` (SIMULATED/RAZORPAY), gateway ids/signature, `transaction_status`, refund fields |
| `doctor_earning` | `appointment_id` **(UNIQUE)** — one ledger row per completed consultation, commission snapshotted |
| `doctor_payout` | one settlement batch per doctor per period; `UNIQUE(doctor, period)` so a re-run can't pay twice |

`doctor_earning` is a **ledger**, not a running balance — any "amount owed" is
a `SUM` over rows, so a correction is a new row, never a destructive update.

### Reviews

| Table | Purpose |
|---|---|
| `rating` | `appointment_id` **(UNIQUE)** — one review per visit; stars (1–5), overall experience, free text |
| `rating_highlight` | junction table for the multi-select "what stood out?" tags |

### Platform / infrastructure

| Table | Purpose |
|---|---|
| `system_settings` | key/value business-rule config — platform fee, cancellation window, commission %, OTP policy, and 20+ others, editable from the admin console without a redeploy |
| `notification` | outbound message log; `UNIQUE(type, entity_type, entity_id, recipient_id, channel)` makes every reminder/confirmation idempotent per channel |
| `activity_log` | audit trail: actor (polymorphic), action, description, timestamp — backs the admin dashboard's activity feed |

---

## 4. Polymorphic references (not real foreign keys)

Four tables reference "whoever did this" without a formal FK, because the
actor can be any of three unrelated identity tables (or the system itself):

- `activity_log.actor_type` + `actor_id`
- `notification.recipient_type` + `recipient_id`
- `refresh_token.user_type` + `user_id`
- `appointment.cancelled_by` / `no_show_by` / `rescheduled_by` (role only, no id — the appointment's own patient/doctor columns already say *who*)

A real FK can't point at "one of three tables", so these are enforced in the
service layer instead. This is a deliberate trade-off, not an oversight — see
`activity_log`'s comment in `V1__schema.sql`.

---

## 5. Notable constraints & why

| Constraint | Table | Protects against |
|---|---|---|
| `UNIQUE(schedule_id)` | appointment | double-booking (the actual guard, not `is_booked`) |
| `UNIQUE(parent_appointment_id)` | appointment | more than one free follow-up per completed visit |
| `UNIQUE(family_member_id, patient_id)` composite FK | appointment, medical_report | pairing patient A with patient B's dependent |
| `UNIQUE(appointment_id)` | doctor_earning | paying a doctor twice for one visit |
| `UNIQUE(appointment_id)` | rating | more than one review per appointment |
| `UNIQUE(appointment_id)` | medical_opinion | two second-opinion documents for one consultation |
| `UNIQUE(doctor, period)` | doctor_payout | settling the same payout period twice |
| `UNIQUE(type, entity, recipient, channel)` | notification | sending the same reminder twice, per channel |
| `UNIQUE(phone_e164)` | patient | two accounts claiming the same phone number |
| `CHECK(drug_a_id < drug_b_id)` | drug_interaction | storing an unordered pair twice, in both directions |
| `CHECK(stars BETWEEN 1 AND 5)` | rating | invalid ratings |
| `CHECK(refund_amount <= amount)` | payment_transaction | over-refunding |
| `CHECK(reschedule_count BETWEEN 0 AND 10)` | appointment | a runaway reschedule loop |

---

## 6. Enum handling

Java enum constants are UPPER_CASE; several MySQL `ENUM` columns hold
lowercase or spaced values (`'active'`, `'Bedside Manner'`, `'PendingPayment'`).
JPA `AttributeConverter`s in `common/enums/converter/` bridge the two. Adding a
new enum-backed column means adding a converter — do not rely on collation to
paper over it.

`AppointmentStatus` additionally maps onto a *third* vocabulary:
`Badge.jsx` on the frontend only colours `pending`/`confirmed`/`cancelled`/`no_show`,
so `AppointmentStatus.toFrontend()` collapses the richer DB states down to
those before anything is sent to a client.

---

## 7. Seed data

- **`DataSeeder`** — the demo admin (`admin@medibridge.com` / `Admin@123`),
  hashed with the real `PasswordEncoder`.
- **`SampleDataSeeder`** — doctors (mostly active, one pending), patients,
  plus appointments, prescriptions, reviews and payments across every state.
  Idempotent: skipped per-row if the row already exists, so re-running it is safe.

Both run in Java (not SQL) so BCrypt hashes come from the same encoder the
login path verifies against, and appointment dates stay relative to "today".

---

See [BUSINESS_LOGIC.md](BUSINESS_LOGIC.md) for what actually happens with
this schema — the booking lifecycle, pricing, cancellation/refund policy,
no-show settlement, payouts, and every other business rule end to end.
