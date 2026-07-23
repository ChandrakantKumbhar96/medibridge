import urllib.request, os, sys

OUT = r"D:\CPROJECT\docs\diagrams"
os.makedirs(OUT, exist_ok=True)

# (filename, kroki-type, source)
diagrams = []

# 1. System Architecture --------------------------------------------------
diagrams.append(("01_system_architecture", "mermaid", r"""
flowchart TB
  subgraph CLIENT["CLIENT TIER — Browser"]
    UI["React 18 SPA<br/>Redux Toolkit · Tailwind · Vite<br/>localhost:5173"]
  end
  subgraph APP["APPLICATION TIER — Spring Boot 4.1 (Java 21)"]
    SEC["Spring Security<br/>JWT + Refresh + OAuth2"]
    CTRL["REST Controllers"]
    SVC["Service Layer<br/>13 feature modules"]
    REPO["Spring Data JPA / Hibernate"]
  end
  DB[("DATA TIER<br/>MySQL 8<br/>20 tables · Flyway")]
  RP["Razorpay<br/>Payment Gateway"]
  GO["Google OAuth2"]
  SMTP["Email / SMTP"]
  UI -- "REST over HTTPS + JWT" --> SEC
  SEC --> CTRL --> SVC --> REPO --> DB
  SVC -- "orders / refunds" --> RP
  SEC -- "verify ID token" --> GO
  SVC -- "meeting links / reminders" --> SMTP
""".strip()))

# 2. ER Diagram -----------------------------------------------------------
diagrams.append(("02_er_diagram", "mermaid", r"""
erDiagram
  SPECIALIZATION ||--o{ DOCTOR : classifies
  DOCTOR ||--o{ DOCTOR_AVAILABILITY : sets
  DOCTOR ||--o{ DOCTOR_SCHEDULE : offers
  PATIENT ||--o{ APPOINTMENT : books
  DOCTOR ||--o{ APPOINTMENT : attends
  DOCTOR_SCHEDULE ||--o| APPOINTMENT : "booked as"
  APPOINTMENT ||--o| CONSULTATION_RECORD : produces
  CONSULTATION_RECORD ||--o| PRESCRIPTION : yields
  PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
  APPOINTMENT ||--o| PAYMENT_TRANSACTION : "paid by"
  APPOINTMENT ||--o| RATING : "reviewed by"
  RATING ||--o{ RATING_HIGHLIGHT : tagged
  APPOINTMENT ||--o| DOCTOR_EARNING : accrues
  DOCTOR_PAYOUT ||--o{ DOCTOR_EARNING : settles
  PATIENT ||--o{ MEDICAL_REPORT : uploads
  PATIENT {
    int patient_id PK
    string full_name
    string email UK
    string password_hash
    date date_of_birth
    string blood_group
    string status
  }
  DOCTOR {
    string doctor_id PK
    string full_name
    string email UK
    int specialization_id FK
    string license_number UK
    decimal consultation_fee
    decimal rating_avg
    string status
  }
  APPOINTMENT {
    int appointment_id PK
    int patient_id FK
    string doctor_id FK
    int schedule_id FK "UNIQUE"
    datetime appointment_date
    string status
    decimal booked_fee
    decimal platform_fee
    decimal total_amount
    string meeting_link
  }
  PAYMENT_TRANSACTION {
    int transaction_id PK
    int appointment_id FK
    decimal amount
    string gateway
    string gateway_order_id
    string transaction_status
    decimal refund_amount
  }
  PRESCRIPTION {
    int prescription_id PK
    int consultation_id FK
    int patient_id FK
    string doctor_id FK
    date date_issued
  }
  DOCTOR_EARNING {
    int earning_id PK
    string doctor_id FK
    int appointment_id FK "UNIQUE"
    decimal gross_amount
    decimal commission_amount
    decimal net_amount
    string status
  }
""".strip()))

# 3. Use Case Diagram -----------------------------------------------------
diagrams.append(("03_use_case", "plantuml", r"""
@startuml
left to right direction
skinparam actorStyle awesome
skinparam packageStyle rectangle

actor "Patient" as P
actor "Doctor" as D
actor "Admin" as A

rectangle "MediBridge Platform" {
  usecase "Register / Login" as UC1
  usecase "Search Doctors" as UC2
  usecase "Book Appointment" as UC3
  usecase "Make Online Payment" as UC4
  usecase "Join Video Consultation" as UC5
  usecase "View / Download Prescription" as UC6
  usecase "Upload Medical Records" as UC7
  usecase "Rate Doctor" as UC8
  usecase "Reschedule / Cancel" as UC9
  usecase "Manage Availability" as UC10
  usecase "Write Prescription" as UC11
  usecase "View Earnings" as UC12
  usecase "Approve / Suspend Doctor" as UC13
  usecase "View Analytics" as UC14
  usecase "Run Payout Settlement" as UC15
  usecase "Manage System Settings" as UC16
}

P --> UC1
P --> UC2
P --> UC3
P --> UC4
P --> UC5
P --> UC6
P --> UC7
P --> UC8
P --> UC9
D --> UC1
D --> UC10
D --> UC11
D --> UC5
D --> UC12
A --> UC1
A --> UC13
A --> UC14
A --> UC15
A --> UC16
@enduml
""".strip()))

# 4. Class Diagram (domain model) -----------------------------------------
diagrams.append(("04_class_diagram", "mermaid", r"""
classDiagram
  class Patient {
    +Integer id
    +String fullName
    +String email
    +LocalDate dateOfBirth
    +String bloodGroup
    +AccountStatus status
  }
  class Doctor {
    +String id
    +String fullName
    +String email
    +String licenseNumber
    +BigDecimal consultationFee
    +BigDecimal ratingAvg
    +AccountStatus status
  }
  class Specialization {
    +Integer id
    +String name
  }
  class DoctorSchedule {
    +Integer id
    +LocalDate availableDate
    +LocalTime startTime
    +Boolean isBooked
  }
  class Appointment {
    +Integer id
    +LocalDateTime appointmentDate
    +AppointmentStatus status
    +BigDecimal totalAmount
    +String meetingLink
  }
  class PaymentTransaction {
    +Integer id
    +BigDecimal amount
    +PaymentStatus status
    +String gatewayOrderId
  }
  class Prescription {
    +Integer id
    +LocalDate dateIssued
  }
  class PrescriptionItem {
    +String medicineName
    +String dosage
    +String frequency
    +String duration
  }
  class DoctorEarning {
    +BigDecimal grossAmount
    +BigDecimal commissionAmount
    +BigDecimal netAmount
  }
  class Rating {
    +Short stars
    +String reviewText
  }
  Specialization "1" --> "*" Doctor
  Doctor "1" --> "*" DoctorSchedule
  Patient "1" --> "*" Appointment
  Doctor "1" --> "*" Appointment
  DoctorSchedule "1" --> "0..1" Appointment
  Appointment "1" --> "0..1" PaymentTransaction
  Appointment "1" --> "0..1" Prescription
  Prescription "1" --> "*" PrescriptionItem
  Appointment "1" --> "0..1" DoctorEarning
  Appointment "1" --> "0..1" Rating
""".strip()))

# 5. Sequence Diagram: booking + payment ----------------------------------
diagrams.append(("05_sequence_booking_payment", "mermaid", r"""
sequenceDiagram
  actor P as Patient
  participant UI as React App
  participant API as Spring Boot API
  participant RP as Razorpay
  participant DB as MySQL

  P->>UI: Select doctor and slot
  UI->>API: POST /appointments
  API->>DB: Create appointment (PENDING_PAYMENT)<br/>hold slot, snapshot fee
  API-->>UI: appointment
  UI->>API: POST /payments/order
  API->>RP: Create order (server-fixed amount)
  RP-->>API: order id
  API-->>UI: order + public key
  UI->>RP: Open checkout, enter payment
  RP-->>UI: payment id + signature
  UI->>API: POST /payments/verify
  API->>API: Verify HMAC-SHA256 signature
  alt signature valid
    API->>DB: Mark PAID, status = ACCEPTED
    API->>API: Generate meeting link, email patient
    API-->>UI: Confirmed
  else signature invalid
    API->>DB: Record FAILED
    API-->>UI: 400 rejected
  end
""".strip()))

# 6. State Diagram: appointment lifecycle ---------------------------------
diagrams.append(("06_appointment_state", "mermaid", r"""
stateDiagram-v2
  [*] --> PENDING_PAYMENT : patient books slot
  PENDING_PAYMENT --> ACCEPTED : payment verified
  PENDING_PAYMENT --> AUTO_EXPIRED : hold expires (job)
  ACCEPTED --> COMPLETED : doctor completes / prescribes
  ACCEPTED --> CANCELLED : cancelled (auto-refund)
  ACCEPTED --> ACCEPTED : reschedule (same id)
  COMPLETED --> [*]
  CANCELLED --> [*]
  AUTO_EXPIRED --> [*]
""".strip()))

# 7. Data Flow Diagram (Level 1) ------------------------------------------
diagrams.append(("07_data_flow_level1", "mermaid", r"""
flowchart LR
  P([Patient])
  D([Doctor])
  A([Admin])
  P1["1.0<br/>Authentication"]
  P2["2.0<br/>Appointment<br/>Booking"]
  P3["3.0<br/>Payment<br/>Processing"]
  P4["4.0<br/>Consultation &<br/>Prescription"]
  P5["5.0<br/>Payout<br/>Settlement"]
  DS1[("Users")]
  DS2[("Appointments")]
  DS3[("Payments")]
  DS4[("Prescriptions")]
  DS5[("Earnings")]
  P -->|credentials| P1 --> DS1
  P -->|slot request| P2 --> DS2
  P2 -->|order| P3 --> DS3
  D -->|diagnosis| P4 --> DS4
  P4 -->|completed| P5 --> DS5
  A -->|run settlement| P5
  DS3 -->|revenue| A
  DS5 -->|net payable| D
""".strip()))

# ---- render each via kroki ---------------------------------------------
FMT = "png"
ok, fail = [], []
for name, dtype, src in diagrams:
    url = f"https://kroki.io/{dtype}/{FMT}"
    req = urllib.request.Request(url, data=src.encode("utf-8"),
                                 headers={"Content-Type": "text/plain"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            data = r.read()
        path = os.path.join(OUT, f"{name}.{FMT}")
        with open(path, "wb") as f:
            f.write(data)
        ok.append(f"{name}.{FMT}  ({len(data):,} bytes)")
    except urllib.error.HTTPError as e:
        fail.append(f"{name}: HTTP {e.code} - {e.read().decode('utf-8', 'ignore')[:200]}")
    except Exception as e:
        fail.append(f"{name}: {e}")

print("RENDERED:")
for x in ok: print("  ok  ", x)
if fail:
    print("FAILED:")
    for x in fail: print("  XX  ", x)
