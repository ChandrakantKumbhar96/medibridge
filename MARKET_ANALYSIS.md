# Telemedicine Market — Feature Analysis

Competitive analysis of India's leading digital healthcare platforms, and how
MediBridge maps against them.

Legend: ✅ built · 🟡 partial · ❌ not built · ⬜ out of scope

---

## 1. Platform-by-platform

### Practo — *the doctor-discovery specialist*
Founded 2007, Bangalore. Strongest at search and booking.

| Feature | MediBridge |
|---|---|
| 1 lakh+ doctor directory, filter by specialty | ✅ |
| Online appointment booking | ✅ |
| Video / voice / **chat** consultation | 🟡 video only |
| Digital prescription after consult | ✅ |
| Health record storage | ✅ |
| Free follow-up chat after consultation | ❌ |
| **Practo Plus** subscription — unlimited consults, family cover, priority booking | ❌ |
| Medicine ordering (up to 40% off) | ⬜ |
| **Practo Pro** — separate doctor-facing app | 🟡 doctor portal in same app |
| Clinic / hospital management (EMR, billing) | ⬜ |

### Apollo 24|7 — *hospital-backed*
Backed by Apollo Hospitals. Strongest at diagnostics and delivery.

| Feature | MediBridge |
|---|---|
| 4,400+ doctors across 55+ specialities | ✅ (model, not scale) |
| Online consultation | ✅ |
| **19-minute medicine delivery** (metros) | ⬜ |
| At-home lab sample collection | ❌ |
| Apollo Diagnostics integration | ❌ |
| Digital health vault | ✅ |
| Physical hospital network linkage | ⬜ |

### Tata 1mg — *diagnostics-led*
Tata Group. Broadest test catalogue.

| Feature | MediBridge |
|---|---|
| 2,000+ blood tests bookable | ❌ |
| Radiology — MRI, CT scan booking | ❌ |
| Medicine ordering by prescription upload | ❌ |
| Teleconsultation | ✅ |
| Specialty programmes (cancer, obesity, at-home vaccination) | ⬜ |
| Health articles / medicine information | ⬜ |

### PharmEasy — *logistics-led*
Largest delivery footprint; 24–48h delivery.

| Feature | MediBridge |
|---|---|
| Medicine subscription / refill reminders | ❌ |
| Diagnostics | ❌ |
| Teleconsultation | ✅ |
| Warehouse + delivery network | ⬜ |

---

## 2. Consolidated feature universe

Everything the market offers, grouped by domain.

### A. Identity & access
| # | Feature | MediBridge |
|---|---|---|
| 1 | Email/password registration | ✅ |
| 2 | Social login (Google) | ✅ |
| 3 | OTP / mobile login | ❌ |
| 4 | **ABHA (Ayushman Bharat Health ID)** | ❌ |
| 5 | Multi-role portals (patient / doctor / admin) | ✅ |
| 6 | JWT + refresh token rotation | ✅ |
| 7 | Family / dependent profiles under one login | ❌ |
| 8 | Two-factor authentication | 🟡 setting exists, not enforced |

### B. Doctor discovery
| # | Feature | MediBridge |
|---|---|---|
| 9 | Search by name / specialty | ✅ |
| 10 | Filter by fee, experience, rating, language, gender | 🟡 specialty only |
| 11 | Doctor profile with bio, qualifications, registration no. | ✅ |
| 12 | Ratings and reviews | ✅ |
| 13 | Verified-doctor badge (licence checked) | ✅ admin approval |
| 14 | Location / clinic-based search | ⬜ |
| 15 | Symptom checker → specialty suggestion | ❌ |

### C. Booking & scheduling
| # | Feature | MediBridge |
|---|---|---|
| 16 | Real-time slot availability | ✅ |
| 17 | Doctor sets weekly availability | ✅ |
| 18 | Auto-generated slots from availability pattern | ✅ |
| 19 | Double-booking prevention | ✅ DB constraint |
| 20 | Slot hold during checkout, auto-release | ✅ |
| 21 | Cancellation with refund policy | ✅ |
| 22 | **Reschedule** | ❌ |
| 23 | Waitlist for full slots | ❌ |
| 24 | Instant / on-demand consultation | ❌ |
| 25 | Recurring or follow-up booking | ❌ |

### D. Consultation
| # | Feature | MediBridge |
|---|---|---|
| 26 | Video consultation | ✅ |
| 27 | Time-boxed join window | ✅ |
| 28 | In-app text chat | ❌ |
| 29 | Voice-only consultation | ❌ |
| 30 | File sharing during consult | 🟡 via records |
| 31 | Free follow-up window (e.g. 7 days) | ❌ |
| 32 | Consultation notes | ✅ |

### E. Clinical records
| # | Feature | MediBridge |
|---|---|---|
| 33 | Structured e-prescription (drug/dose/frequency/duration) | ✅ |
| 34 | Prescription PDF download | ✅ |
| 35 | Medical history PDF | ✅ |
| 36 | Document upload (reports, scans) | ✅ |
| 37 | Health vault / timeline view | 🟡 list, not timeline |
| 38 | Doctor access to patient history | ✅ scoped to treated patients |
| 39 | ABDM-compliant record sharing with consent | ❌ |
| 40 | Vitals tracking (BP, sugar, weight) | ❌ |

### F. Payments & money
| # | Feature | MediBridge |
|---|---|---|
| 41 | Online payment gateway | ✅ Razorpay |
| 42 | Server-side signature verification | ✅ |
| 43 | Price locked at booking | ✅ |
| 44 | Platform fee / commission | ✅ charged |
| 45 | Automatic refunds on cancellation | ✅ |
| 46 | Partial refund by cancellation window | ✅ |
| 47 | Invoice / receipt | 🟡 record exists, no PDF |
| 48 | **Doctor payout & settlement** | ❌ |
| 49 | Wallet / credits | ❌ |
| 50 | Coupons and discounts | ❌ |
| 51 | Subscription plans | ❌ |
| 52 | Insurance claim integration | ⬜ |

### G. Notifications
| # | Feature | MediBridge |
|---|---|---|
| 53 | Email on booking / confirm / cancel | ✅ |
| 54 | Appointment reminders (24h) | ✅ scheduled job |
| 55 | Send de-duplication | ✅ unique key |
| 56 | SMS notifications | ❌ |
| 57 | Push notifications | ❌ |
| 58 | WhatsApp notifications | ❌ |

### H. Admin & operations
| # | Feature | MediBridge |
|---|---|---|
| 59 | Doctor verification / approval | ✅ |
| 60 | Suspend / reinstate accounts | ✅ |
| 61 | Platform dashboard with live metrics | ✅ |
| 62 | Revenue analytics | ✅ |
| 63 | Audit trail (login, status changes) | ✅ |
| 64 | Configurable business rules (fees, windows) | ✅ DB-backed |
| 65 | CSV report export | ✅ |
| 66 | Support ticketing | ❌ |
| 67 | Content moderation for reviews | ❌ |

### I. Ancillary services
| # | Feature | MediBridge |
|---|---|---|
| 68 | Lab test booking | ❌ |
| 69 | At-home sample collection | ⬜ |
| 70 | Radiology (MRI / CT) booking | ⬜ |
| 71 | Medicine ordering | ⬜ |
| 72 | Medicine delivery / logistics | ⬜ |
| 73 | Refill reminders | ❌ |
| 74 | Health articles | ⬜ |
| 75 | At-home vaccination / nursing | ⬜ |

---

## 3. Scorecard

| Domain | Coverage |
|---|---|
| Identity & access | 4 / 8 |
| Doctor discovery | 5 / 7 |
| Booking & scheduling | 6 / 10 |
| Consultation | 3 / 7 |
| Clinical records | 6 / 8 |
| Payments | 7 / 12 |
| Notifications | 3 / 6 |
| Admin & operations | 7 / 9 |
| Ancillary | 0 / 8 |
| **Core platform (excl. ancillary)** | **41 / 67 ≈ 61%** |

The ancillary block (pharmacy, diagnostics, logistics) is a different business,
not a missing feature — those need supplier contracts and delivery fleets, not
code. Judged on the consultation platform itself, coverage is ~61%, and the
gaps are mostly breadth rather than depth.

---

## 4. Where MediBridge is unusually strong

Several things here are engineered more carefully than a feature list suggests:

- **Double-booking is prevented by a DB constraint**, not an application check.
  Two simultaneous bookings cannot both succeed regardless of timing.
- **Payment amount is snapshotted at booking.** A doctor raising their fee
  afterwards cannot change what an already-booked patient owes.
- **Payment signatures are verified server-side.** A forged success callback is
  rejected, and the attempt is recorded as FAILED in its own transaction so the
  audit survives the rollback.
- **Ownership checks return 404, not 403** — a 403 would confirm the record exists.
- **Abandoned checkouts self-heal.** A scheduled sweep releases held slots.
- **Business rules are configurable** — platform fee, hold duration, cancellation
  window and refund percentage all live in `system_settings`.

---

## 5. Recommended additions, ranked

| Priority | Feature | Why |
|---|---|---|
| 1 | **Doctor payout & settlement** | Completes the money story: ledger, commission split, reconciliation |
| 2 | **Reschedule** | State-machine problem — release old slot, hold new, carry payment, atomically |
| 3 | **ABHA / ABDM integration** | India-specific, government-backed, strong differentiator |
| 4 | Family / dependent profiles | Data-modelling depth: one login, many patients |
| 5 | Idempotency keys on payment | Prevents double-charge on network retry |
| 6 | Rate limiting on login | Brute-force protection |
| 7 | In-app chat (WebSocket) | Demonstrates a protocol beyond REST |
| 8 | Invoice PDF | Template engine already in place |
| 9 | Lab test booking | Extends the catalogue model |
| 10 | SMS / WhatsApp notifications | Channel abstraction already exists |

---

## Sources

- [Practo](https://www.practo.com/) · [Practo Plus FAQ](https://help.practo.com/practo-plus/faqs-for-practo-plus/)
- [Apollo 24|7](https://www.apollo247.com/)
- [Tata 1mg Labs](https://www.1mg.com/labs)
- [Onsurity — top telemedicine apps in India](https://www.onsurity.com/blog/telemedicine-apps-in-india/)
- [ABHA integration guide](https://productgrowth.in/insights/healthtech/abha-integration-guide/)
- [Telemedicine regulations in India 2026](https://doccure.io/telemedicine-regulations-in-india-2026-updated-guidelines-and-compliance-tips-for-clinics/)
