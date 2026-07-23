# MediBridge — Documentation

Full technical documentation for the MediBridge telemedicine platform.

| Document | Covers |
|---|---|
| [BACKEND.md](BACKEND.md) | Spring Boot architecture, modules, security, business rules, how to run |
| [FRONTEND.md](FRONTEND.md) | React structure, Redux state, routing, service layer, how to run |
| [API_REFERENCE.md](API_REFERENCE.md) | Every REST endpoint, grouped by domain, with roles and error shapes |
| [DATABASE.md](DATABASE.md) | MySQL schema, all 20 tables, relationships, constraints |

Also at the repository root:

| Document | Covers |
|---|---|
| [../README.md](../README.md) | Project overview and quick start |
| [../CONNECTIVITY.md](../CONNECTIVITY.md) | Frontend ↔ backend endpoint mapping per screen |
| [../MARKET_ANALYSIS.md](../MARKET_ANALYSIS.md) | Competitive feature analysis vs Practo / Apollo / 1mg |

---

## System at a glance

```
┌─────────────────────┐         ┌──────────────────────────┐         ┌───────────┐
│  React SPA           │  HTTP   │  Spring Boot 4.1 REST API │  JDBC   │  MySQL 8  │
│  Vite · Redux ·      │ ──────► │  Java 21 · Spring Security│ ──────► │  Flyway   │
│  Tailwind · Axios    │  :5173  │  JPA/Hibernate · :8080/api│         │  20 tables│
└─────────────────────┘         └──────────────────────────┘         └───────────┘
        │                                    │
        │ Razorpay Checkout                  ├── Razorpay (payments + refunds)
        │ Google Sign-In                     ├── Google OAuth2 (ID-token verify)
        └────────────────────────────────────┴── Email (meeting links, reminders)
```

**Architecture:** modular monolith · three role-based portals (Patient, Doctor,
Admin) · JWT auth with refresh-token rotation · slot-based booking with
double-booking prevention · online payments with server-side signature
verification · e-prescriptions and reports as PDF · doctor earnings and payout
settlement.
