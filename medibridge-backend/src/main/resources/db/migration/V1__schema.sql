-- =============================================================================
-- MediBridge - schema v1
-- Based on the team ER design, with corrections noted per table.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. SPECIALIZATION  (new - fixes free-text mismatch between
--    'Cardiology' in the register dropdown and 'Cardiologist' on doctor rows)
-- -----------------------------------------------------------------------------
CREATE TABLE `specialization` (
  `specialization_id` INT AUTO_INCREMENT PRIMARY KEY,
  `name`              VARCHAR(100) NOT NULL UNIQUE,
  `emoji`             VARCHAR(16)  DEFAULT NULL,
  `description`       VARCHAR(255) DEFAULT NULL,
  `created_at`        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. ADMIN
--    `username` dropped: the admin login screen collects email only and there
--    is no admin registration form, so the column could never be populated.
-- -----------------------------------------------------------------------------
CREATE TABLE `admin` (
  `admin_id`      INT AUTO_INCREMENT PRIMARY KEY,
  `full_name`     VARCHAR(100) NOT NULL,
  `email`         VARCHAR(100) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `title`         VARCHAR(50)  DEFAULT 'System Administrator',
  `status`        ENUM('active','inactive') DEFAULT 'active',
  `created_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. DOCTOR
--    CHAR(36) UUID (fixed width beats VARCHAR for a PK carried into 6 FKs).
--    `specialization` is now an FK instead of free text.
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor` (
  `doctor_id`                 CHAR(36) PRIMARY KEY,
  `full_name`                 VARCHAR(100) NOT NULL,
  `email`                     VARCHAR(100) NOT NULL UNIQUE,
  `password_hash`             VARCHAR(255) NOT NULL,
  `phone`                     VARCHAR(20)  NOT NULL,
  `specialization_id`         INT          NOT NULL,
  `license_number`            VARCHAR(50)  NOT NULL UNIQUE,
  `experience_years`          INT          NOT NULL,
  `consultation_fee`          DECIMAL(10,2) NOT NULL,
  `consultation_duration_min` INT          DEFAULT 30,
  `bio`                       TEXT,
  `rating_avg`                DECIMAL(3,2) DEFAULT 0.00,
  `rating_count`              INT          DEFAULT 0,
  -- 'pending' is the state a self-registered doctor lands in before admin approval
  `status`                    ENUM('pending','active','inactive','suspended') DEFAULT 'pending',
  `created_at`                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`specialization_id`) REFERENCES `specialization`(`specialization_id`)
    ON DELETE RESTRICT,
  INDEX `idx_doctor_specialization` (`specialization_id`),
  INDEX `idx_doctor_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. PATIENT   (fields match PatientRegisterForm.jsx exactly)
-- -----------------------------------------------------------------------------
CREATE TABLE `patient` (
  `patient_id`        INT AUTO_INCREMENT PRIMARY KEY,
  `full_name`         VARCHAR(100) NOT NULL,
  `email`             VARCHAR(100) NOT NULL UNIQUE,
  `password_hash`     VARCHAR(255) NOT NULL,
  `phone`             VARCHAR(20)  NOT NULL,
  `another_number`    VARCHAR(20)  DEFAULT NULL,
  `address`           TEXT,
  `date_of_birth`     DATE         NOT NULL,
  `gender`            ENUM('Male','Female','Other') NOT NULL,
  `blood_group`       VARCHAR(5)   NOT NULL,
  `reason_of_consult` TEXT,
  `status`            ENUM('active','inactive') DEFAULT 'active',
  `created_at`        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. DOCTOR_AVAILABILITY  (new - the recurring weekly pattern the
--    Manage Schedule screen actually edits: day + morning/afternoon)
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor_availability` (
  `availability_id` INT AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`       CHAR(36) NOT NULL,
  `day_of_week`     ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY',
                         'FRIDAY','SATURDAY','SUNDAY') NOT NULL,
  `is_available`    BOOLEAN DEFAULT TRUE,
  `morning`         BOOLEAN DEFAULT FALSE,   -- 09:00-12:00
  `afternoon`       BOOLEAN DEFAULT FALSE,   -- 14:00-17:00
  UNIQUE KEY `uq_doctor_day` (`doctor_id`, `day_of_week`),
  FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`doctor_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. DOCTOR_SCHEDULE  (concrete dated slots generated from the weekly template,
--    sliced by the doctor's consultation_duration_min)
--    `is_booked` is a denormalised read cache only - never the booking guard.
--    The real guard is the UNIQUE constraint on appointment.schedule_id below.
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor_schedule` (
  `schedule_id`    INT AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`      CHAR(36) NOT NULL,
  `available_date` DATE     NOT NULL,
  `start_time`     TIME     NOT NULL,
  `end_time`       TIME     NOT NULL,
  `is_booked`      BOOLEAN  DEFAULT FALSE,
  UNIQUE KEY `uq_doctor_slot` (`doctor_id`, `available_date`, `start_time`),
  FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`doctor_id`) ON DELETE CASCADE,
  INDEX `idx_schedule_lookup` (`doctor_id`, `available_date`, `is_booked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. APPOINTMENT
--    UNIQUE(schedule_id) is what actually prevents double-booking: two
--    concurrent confirms both pass an is_booked check, but only one can insert.
--    `meeting_link` holds the generated consultation URL emailed to the patient.
-- -----------------------------------------------------------------------------
CREATE TABLE `appointment` (
  `appointment_id`   INT AUTO_INCREMENT PRIMARY KEY,
  `patient_id`       INT      NOT NULL,
  `doctor_id`        CHAR(36) NOT NULL,
  `schedule_id`      INT      DEFAULT NULL,
  `request_date`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  `appointment_date` DATETIME NOT NULL,
  `status`           ENUM('PendingPayment','Requested','Accepted','Rejected',
                          'Rescheduled','Completed','Cancelled','AutoExpired')
                     DEFAULT 'PendingPayment',
  `consult_type`     VARCHAR(50) DEFAULT 'Consultation',
  `reason`           TEXT,
  `meeting_link`     VARCHAR(500) DEFAULT NULL,
  `meeting_sent_at`  DATETIME     DEFAULT NULL,
  `created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_schedule_booking` (`schedule_id`),
  FOREIGN KEY (`patient_id`)  REFERENCES `patient`(`patient_id`)   ON DELETE RESTRICT,
  FOREIGN KEY (`doctor_id`)   REFERENCES `doctor`(`doctor_id`)     ON DELETE RESTRICT,
  FOREIGN KEY (`schedule_id`) REFERENCES `doctor_schedule`(`schedule_id`) ON DELETE SET NULL,
  INDEX `idx_appt_patient`  (`patient_id`, `status`),
  INDEX `idx_appt_doctor`   (`doctor_id`, `appointment_date`),
  INDEX `idx_appt_date`     (`appointment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. MEDICAL_REPORT
--    uploaded_by_* added: the doctor portal lists patient records, so both the
--    UI and the authorization check need to know who uploaded each file.
-- -----------------------------------------------------------------------------
CREATE TABLE `medical_report` (
  `report_id`        INT AUTO_INCREMENT PRIMARY KEY,
  `patient_id`       INT          NOT NULL,
  `report_name`      VARCHAR(150) NOT NULL,
  `report_type`      VARCHAR(50)  NOT NULL,
  `report_data_url`  VARCHAR(255) NOT NULL,
  `content_type`     VARCHAR(100) DEFAULT NULL,
  `file_size_bytes`  BIGINT       DEFAULT 0,
  `uploaded_by_type` ENUM('PATIENT','DOCTOR','SYSTEM') DEFAULT 'PATIENT',
  `uploaded_by_id`   VARCHAR(36)  DEFAULT NULL,
  `upload_date`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`patient_id`) REFERENCES `patient`(`patient_id`) ON DELETE CASCADE,
  INDEX `idx_report_patient` (`patient_id`, `upload_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. CONSULTATION_RECORD
--    `has_prescription` removed - it duplicated the existence of a prescription
--    row and would drift. Derive it with a join instead.
-- -----------------------------------------------------------------------------
CREATE TABLE `consultation_record` (
  `consultation_id` INT AUTO_INCREMENT PRIMARY KEY,
  `appointment_id`  INT          NOT NULL UNIQUE,
  `diagnosis`       VARCHAR(255) NOT NULL,
  `notes`           TEXT,
  `follow_up_date`  DATE         DEFAULT NULL,
  `created_at`      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`appointment_id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 10. PRESCRIPTION
--     `diagnosis_text` removed - consultation_record.diagnosis is the source.
-- -----------------------------------------------------------------------------
CREATE TABLE `prescription` (
  `prescription_id` INT AUTO_INCREMENT PRIMARY KEY,
  `consultation_id` INT      NOT NULL UNIQUE,
  `patient_id`      INT      NOT NULL,
  `doctor_id`       CHAR(36) NOT NULL,
  `date_issued`     DATE     NOT NULL,
  `advice`          TEXT,
  `created_at`      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`consultation_id`) REFERENCES `consultation_record`(`consultation_id`)
    ON DELETE CASCADE,
  FOREIGN KEY (`patient_id`) REFERENCES `patient`(`patient_id`) ON DELETE RESTRICT,
  FOREIGN KEY (`doctor_id`)  REFERENCES `doctor`(`doctor_id`)   ON DELETE RESTRICT,
  INDEX `idx_prescription_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 11. PRESCRIPTION_ITEM  (new - REQUIRED for the prescription PDF.
--     Without this the PDF has a diagnosis and no medicines.)
-- -----------------------------------------------------------------------------
CREATE TABLE `prescription_item` (
  `item_id`         INT AUTO_INCREMENT PRIMARY KEY,
  `prescription_id` INT          NOT NULL,
  `medicine_name`   VARCHAR(150) NOT NULL,
  `dosage`          VARCHAR(50)  NOT NULL,   -- '500 mg'
  `frequency`       VARCHAR(50)  NOT NULL,   -- '1-0-1'
  `duration`        VARCHAR(50)  NOT NULL,   -- '5 days'
  `instructions`    VARCHAR(255) DEFAULT NULL,
  `sort_order`      INT          DEFAULT 0,
  FOREIGN KEY (`prescription_id`) REFERENCES `prescription`(`prescription_id`)
    ON DELETE CASCADE,
  INDEX `idx_item_prescription` (`prescription_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 12. PAYMENT_TRANSACTION
--     'Pending' added: the appointment row is created before payment settles.
-- -----------------------------------------------------------------------------
CREATE TABLE `payment_transaction` (
  `transaction_id`     INT AUTO_INCREMENT PRIMARY KEY,
  `appointment_id`     INT           NOT NULL,
  `amount`             DECIMAL(10,2) NOT NULL,
  `payment_method`     VARCHAR(50)   NOT NULL,
  `transaction_ref`    VARCHAR(64)   NOT NULL UNIQUE,
  `transaction_status` ENUM('Pending','Paid','Refunded','Failed') DEFAULT 'Pending',
  `refund_amount`      DECIMAL(10,2) DEFAULT NULL,
  `refunded_at`        DATETIME      DEFAULT NULL,
  `processed_at`       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`appointment_id`)
    ON DELETE RESTRICT,
  INDEX `idx_txn_appointment` (`appointment_id`),
  INDEX `idx_txn_status_date` (`transaction_status`, `processed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 13. RATING
--     doctor_id FK changed CASCADE -> RESTRICT for consistency with appointment.
-- -----------------------------------------------------------------------------
CREATE TABLE `rating` (
  `rating_id`          INT AUTO_INCREMENT PRIMARY KEY,
  `appointment_id`     INT      NOT NULL UNIQUE,
  `patient_id`         INT      NOT NULL,
  `doctor_id`          CHAR(36) NOT NULL,
  `stars`              TINYINT  NOT NULL CHECK (`stars` BETWEEN 1 AND 5),
  `overall_experience` ENUM('Excellent','Good','Okay','Poor') NOT NULL,
  `review_text`        TEXT,
  `created_at`         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`appointment_id`) ON DELETE RESTRICT,
  FOREIGN KEY (`patient_id`)     REFERENCES `patient`(`patient_id`)         ON DELETE RESTRICT,
  FOREIGN KEY (`doctor_id`)      REFERENCES `doctor`(`doctor_id`)           ON DELETE RESTRICT,
  INDEX `idx_rating_doctor` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 14. RATING_HIGHLIGHT  (new - "What stood out?" is multi-select in
--     RateExperience.jsx, so a single ENUM column would drop all but one tag)
-- -----------------------------------------------------------------------------
CREATE TABLE `rating_highlight` (
  `rating_id` INT NOT NULL,
  `highlight` ENUM('Bedside Manner','Clear Explanations','Follow-up Care',
                   'Accurate Diagnosis','Friendly Staff') NOT NULL,
  PRIMARY KEY (`rating_id`, `highlight`),
  FOREIGN KEY (`rating_id`) REFERENCES `rating`(`rating_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 15. SYSTEM_SETTINGS  (new - backs the admin System Settings screen)
-- -----------------------------------------------------------------------------
CREATE TABLE `system_settings` (
  `setting_key`   VARCHAR(64)  PRIMARY KEY,
  `setting_value` VARCHAR(255) NOT NULL,
  `value_type`    ENUM('STRING','INT','BOOLEAN') DEFAULT 'STRING',
  `updated_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 16. REFRESH_TOKEN  (new - revocable logout; only the hash is stored)
-- -----------------------------------------------------------------------------
CREATE TABLE `refresh_token` (
  `token_id`    INT AUTO_INCREMENT PRIMARY KEY,
  `token_hash`  CHAR(64)    NOT NULL UNIQUE,
  `user_type`   ENUM('PATIENT','DOCTOR','ADMIN') NOT NULL,
  `user_id`     VARCHAR(36) NOT NULL,
  `expires_at`  DATETIME    NOT NULL,
  `revoked`     BOOLEAN     DEFAULT FALSE,
  `created_at`  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_refresh_user` (`user_type`, `user_id`),
  INDEX `idx_refresh_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 17. ACTIVITY_LOG  (new - audit trail + the admin dashboard's activity feed)
-- -----------------------------------------------------------------------------
CREATE TABLE `activity_log` (
  `log_id`      BIGINT AUTO_INCREMENT PRIMARY KEY,
  `actor_type`  ENUM('PATIENT','DOCTOR','ADMIN','SYSTEM') NOT NULL,
  `actor_id`    VARCHAR(36)  DEFAULT NULL,
  `actor_name`  VARCHAR(100) DEFAULT NULL,
  `action`      VARCHAR(64)  NOT NULL,   -- LOGIN, LOGOUT, BOOK_APPOINTMENT, ...
  `description` VARCHAR(255) DEFAULT NULL,
  `entity_type` VARCHAR(50)  DEFAULT NULL,
  `entity_id`   VARCHAR(36)  DEFAULT NULL,
  `ip_address`  VARCHAR(45)  DEFAULT NULL,
  `created_at`  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_activity_created` (`created_at`),
  INDEX `idx_activity_actor` (`actor_type`, `actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
