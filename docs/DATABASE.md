# MediBridge — Database Documentation

MySQL 8 schema, owned and versioned by **Flyway** (migrations V1–V5).
20 tables. Character set `utf8mb4`, engine InnoDB.

---

## 1. Migration history

| Version | Adds |
|---|---|
| **V1** | Core schema — identity, doctors, appointments, records, prescriptions, payments, reviews, settings, audit |
| **V2** | Reference data — specializations, system settings |
| **V3** | Google OAuth columns on patient; payment-gateway columns |
| **V4** | Money integrity (fee snapshots), slot holds, meeting windows, notification table, CHECK constraints |
| **V5** | Doctor earnings ledger, payout batches, reschedule tracking |

`ddl-auto: validate` runs at startup — Hibernate confirms every entity matches
its table, catching drift before it becomes corrupt data.

---

## 2. Tables by domain

### Identity (3 separate tables)

| Table | Purpose | Key columns |
|---|---|---|
| `patient` | patient accounts | id, email, password_hash, dob, gender, blood_group, auth_provider, google_sub, status |
| `doctor` | doctor accounts + profile | id (UUID), email, specialization_id (FK), license_number, consultation_fee, rating_avg, status |
| `admin` | admin accounts | id, email, password_hash, status |
| `refresh_token` | hashed refresh tokens | token_hash, user_type, user_id, expires_at, revoked |

Three identity tables, each with its own `password_hash`. `doctor.id` is a UUID
(so public doctor listings can't be enumerated); patient/admin ids are ints.

### Doctors & scheduling

| Table | Purpose |
|---|---|
| `specialization` | lookup: name, emoji, description |
| `doctor_availability` | recurring weekly pattern (day, morning, afternoon) |
| `doctor_schedule` | concrete bookable slots generated from availability; `UNIQUE(doctor, date, start_time)` |

### Appointments

| Table | Key columns |
|---|---|
| `appointment` | patient_id, doctor_id, schedule_id **(UNIQUE)**, status, booked_fee, platform_fee, total_amount, hold_expires_at, meeting_link, meeting_join_from/until, reschedule_count, cancelled_by |

`UNIQUE(schedule_id)` is the real double-booking guard. Fee columns are
snapshotted at booking so a later price change can't alter what's owed.

### Clinical records

| Table | Purpose |
|---|---|
| `consultation_record` | one per appointment: diagnosis, notes, follow_up_date |
| `prescription` | one per consultation: patient, doctor, date_issued, advice |
| `prescription_item` | medicines: name, dosage, frequency, duration, instructions |
| `medical_report` | uploaded documents: name, type, file path, uploaded_by |

### Payments & payouts

| Table | Key columns |
|---|---|
| `payment_transaction` | appointment_id, amount, consultation_amount, platform_fee, gateway, gateway_order_id, gateway_payment_id, transaction_status, refund_amount, gateway_refund_id |
| `doctor_earning` | appointment_id **(UNIQUE)**, gross_amount, commission_rate, commission_amount, net_amount, status, payout_id |
| `doctor_payout` | doctor, period_start/end, consultations, gross/commission/net, status, payout_ref; `UNIQUE(doctor, period)` |

`doctor_earning` is a ledger (one row per consultation), not a running balance —
any balance is a `SUM`. Commission rate is snapshotted per row.

### Reviews

| Table | Purpose |
|---|---|
| `rating` | appointment_id **(UNIQUE)**, patient, doctor, stars (1–5), overall_experience, review_text |
| `rating_highlight` | multi-select tags per rating (junction table) |

`rating_highlight` is a separate table because "what stood out" is multi-select —
a single ENUM column would drop all but one tag.

### Platform

| Table | Purpose |
|---|---|
| `system_settings` | key/value config: platform_fee, slot_hold_minutes, free_cancellation_hours, partial_refund_percent, commission_percent, payout_cycle_days, etc. |
| `notification` | outbound message log; `UNIQUE(type, entity, recipient)` makes reminders idempotent |
| `activity_log` | audit trail: actor, action, description, timestamp |

---

## 3. Key relationships

```
patient ──< appointment >── doctor
                │              │
                │              ├──< doctor_availability
                │              ├──< doctor_schedule ──┐ (UNIQUE on appointment.schedule_id)
                │              └──< doctor_earning >── doctor_payout
                │
                ├── consultation_record ── prescription ──< prescription_item
                ├── payment_transaction
                └── rating ──< rating_highlight

doctor >── specialization
patient ──< medical_report
```

- `>──` = many-to-one, `──<` = one-to-many.
- Appointment sits at the centre; it links a patient and doctor and fans out to
  the consultation, payment, earning, and rating.

---

## 4. Notable constraints & why

| Constraint | Table | Protects against |
|---|---|---|
| `UNIQUE(schedule_id)` | appointment | double-booking (the real guard) |
| `UNIQUE(appointment_id)` | doctor_earning | paying a doctor twice for one visit |
| `UNIQUE(appointment_id)` | rating | more than one review per appointment |
| `UNIQUE(doctor, period)` | doctor_payout | settling the same period twice |
| `UNIQUE(type,entity,recipient)` | notification | sending the same reminder twice |
| `CHECK(stars BETWEEN 1 AND 5)` | rating | invalid ratings |
| `CHECK(refund ≤ amount)` | payment_transaction | over-refunding |
| `CHECK(gross = commission + net)` enforced in code | doctor_earning | money "leaking" in rounding |

---

## 5. Enum handling

Java enum constants are UPPER_CASE; several MySQL `ENUM` columns hold lowercase
or spaced values (`'active'`, `'Bedside Manner'`). JPA `AttributeConverter`s in
`common/enums/converter/` bridge the two. Adding a new enum-backed column means
adding a converter — do not rely on the collation to paper over it.

---

## 6. Seed data

- **`DataSeeder`** — the demo admin (`admin@medibridge.com` / `Admin@123`),
  hashed with the real `PasswordEncoder`.
- **`SampleDataSeeder`** — 6 doctors (5 active, 1 pending), 4 patients, plus
  appointments, prescriptions, reviews and payments across every state.
  Idempotent: skipped if any doctor already exists.

Both run in Java (not SQL) so BCrypt hashes come from the same encoder the login
path verifies against, and appointment dates stay relative to "today".
