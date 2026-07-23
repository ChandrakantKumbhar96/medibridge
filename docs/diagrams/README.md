# MediBridge — Diagrams

UML and architecture diagrams for the MediBridge platform, as PNG images ready to
embed in a report.

| # | File | Diagram | What it shows |
|---|---|---|---|
| 1 | `01_system_architecture.png` | System Architecture | 3-tier design (React ↔ Spring Boot ↔ MySQL) + Razorpay, Google OAuth, Email |
| 2 | `02_er_diagram.png` | Entity-Relationship | Core tables, keys (PK/FK/UK) and cardinalities |
| 3 | `03_use_case.png` | Use Case | Patient, Doctor and Admin actors with their use cases |
| 4 | `04_class_diagram.png` | Class / Domain Model | Key entities, fields and associations |
| 5 | `05_sequence_booking_payment.png` | Sequence | Booking → payment → confirmation flow, incl. signature verification |
| 6 | `06_appointment_state.png` | State Machine | Appointment status lifecycle |
| 7 | `07_data_flow_level1.png` | Data Flow (Level 1) | Processes, external entities and data stores |

## Regenerating

The diagram sources live in `scripts/gen_diagrams.py` (Mermaid + PlantUML), rendered
to PNG via the kroki.io API. Re-run it to regenerate after schema or flow changes.

Need a different format (SVG for print, higher resolution) or an extra diagram
(deployment, package, activity)? Change the format string or add a block in the
script and re-run.
