# MediBridge — Documentation

Start at the [repository root README](../README.md) — it's the single source
of truth for architecture, what each service is responsible for, and how a
request flows through the system end to end. This folder holds the deep dives
that would make that README too long to be a README.

| Document | Covers |
|---|---|
| [PRESENTATION.md](PRESENTATION.md) | Full project write-up with Mermaid diagrams — DFDs, ER diagram, use-case, sequence, state machine, code-flow walkthroughs. Written for a project viva/presentation. |
| [DATABASE.md](DATABASE.md) | MySQL schema — all 26 tables, migration history (Flyway V1–V15), relationships, constraints |
| [BUSINESS_LOGIC.md](BUSINESS_LOGIC.md) | Every business rule end to end and *why* it exists — booking, pricing, cancellation/refund, reschedule, no-show settlement, payouts, and more |

Also at the repository root:

| Document | Covers |
|---|---|
| [../README.md](../README.md) | Project overview, architecture, service responsibilities, request-flow walkthrough, quick start |
| [../MARKET_ANALYSIS.md](../MARKET_ANALYSIS.md) | Competitive feature analysis vs Practo / Apollo / 1mg |

---

## System at a glance

```
┌───────────┐  REST+JWT  ┌────────────────┐         ┌───────────────────┐         ┌───────────┐
│ React SPA │ ─────────► │ Node/Express    │  /api   │ Spring Boot 4.1    │  JDBC   │ MySQL 8   │
│ :5173     │            │ Gateway :4000   │ ──────► │ Java 21 :8080/api  │ ──────► │ 26 tables │
└───────────┘            │ JWT verify ·    │         └────────────────────┘         │ Flyway    │
                          │ rate limit ·    │  /api/chat, /api/triage
                          │ circuit breaker │ ──────► Python/FastAPI RAG chat :8000
                          └────────┬────────┘
                                   │ /api/reports
                                   └────────► .NET Notify Service :5154 (SMS + CSV)
```

**Architecture:** a modular monolith (Spring Boot) for the money/booking path,
with three satellite services — a Node gateway, a Python RAG chat service, and
a .NET reminders/reports service — each in the language that fits the job.
Three role-based portals (Patient, Doctor, Admin), JWT auth with refresh-token
rotation, slot-based booking with database-enforced double-booking prevention,
server-side-verified online payments, e-prescriptions and reports as PDF, and
doctor earnings/payout settlement. Full breakdown in the
[root README](../README.md#architecture).
