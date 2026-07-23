-- =============================================================================
-- V5 - Doctor earnings ledger, payout settlement, and reschedule support
--
-- Until now the platform collected money but had no record of what it OWED.
-- A marketplace needs both halves: what the patient paid, and what the doctor
-- is due after commission.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- DOCTOR_PAYOUT - a settlement batch covering one doctor for one period.
-- Created first because doctor_earning references it.
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor_payout` (
  `payout_id`      INT AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`      CHAR(36)      NOT NULL,
  `period_start`   DATE          NOT NULL,
  `period_end`     DATE          NOT NULL,
  `consultations`  INT           NOT NULL DEFAULT 0,
  `gross_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `commission`     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `net_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `status`         ENUM('PENDING','PAID','FAILED') NOT NULL DEFAULT 'PENDING',
  `payout_ref`     VARCHAR(64)   DEFAULT NULL,
  `notes`          VARCHAR(255)  DEFAULT NULL,
  `created_at`     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  `paid_at`        DATETIME      DEFAULT NULL,
  FOREIGN KEY (`doctor_id`) REFERENCES `doctor`(`doctor_id`) ON DELETE RESTRICT,
  -- One settlement per doctor per period: re-running the job cannot pay twice.
  UNIQUE KEY `uq_payout_period` (`doctor_id`, `period_start`, `period_end`),
  INDEX `idx_payout_status` (`status`, `created_at`),
  CONSTRAINT `chk_payout_amounts` CHECK (
        `gross_amount` >= 0 AND `commission` >= 0 AND `net_amount` >= 0),
  CONSTRAINT `chk_payout_period` CHECK (`period_end` >= `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- DOCTOR_EARNING - one row per completed consultation.
--
-- A ledger, not a running total. Storing a single "balance" column on the
-- doctor row would make every correction a destructive update with no history;
-- here each consultation's economics are permanent and auditable, and any
-- balance is a SUM over rows.
--
-- commission_rate is snapshotted for the same reason booked_fee is: changing
-- the platform's rate must not silently rewrite what past consultations earned.
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor_earning` (
  `earning_id`       INT AUTO_INCREMENT PRIMARY KEY,
  `doctor_id`        CHAR(36)      NOT NULL,
  `appointment_id`   INT           NOT NULL,
  -- The consultation fee only. The platform fee the patient paid on top is
  -- platform revenue and never enters the doctor's ledger.
  `gross_amount`     DECIMAL(10,2) NOT NULL,
  `commission_rate`  DECIMAL(5,2)  NOT NULL,
  `commission_amount` DECIMAL(10,2) NOT NULL,
  `net_amount`       DECIMAL(10,2) NOT NULL,
  `status`           ENUM('PENDING','SETTLED','REVERSED') NOT NULL DEFAULT 'PENDING',
  `payout_id`        INT           DEFAULT NULL,
  `reversal_reason`  VARCHAR(255)  DEFAULT NULL,
  `earned_at`        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  `settled_at`       DATETIME      DEFAULT NULL,
  FOREIGN KEY (`doctor_id`)      REFERENCES `doctor`(`doctor_id`)           ON DELETE RESTRICT,
  -- One earning per appointment - the guard against paying twice for one visit.
  FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`appointment_id`) ON DELETE RESTRICT,
  FOREIGN KEY (`payout_id`)      REFERENCES `doctor_payout`(`payout_id`)    ON DELETE SET NULL,
  UNIQUE KEY `uq_earning_appointment` (`appointment_id`),
  INDEX `idx_earning_doctor_status` (`doctor_id`, `status`),
  INDEX `idx_earning_payout` (`payout_id`),
  CONSTRAINT `chk_earning_amounts` CHECK (
        `gross_amount` >= 0
    AND `commission_amount` >= 0
    AND `net_amount` >= 0
    AND `commission_rate` BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- APPOINTMENT - reschedule tracking.
--
-- The appointment id is deliberately preserved across a reschedule: the payment,
-- the prescription and the audit trail all reference it, and minting a new row
-- would orphan them.
-- -----------------------------------------------------------------------------
ALTER TABLE `appointment`
  ADD COLUMN `reschedule_count`  TINYINT  NOT NULL DEFAULT 0 AFTER `cancellation_reason`,
  ADD COLUMN `original_date`     DATETIME DEFAULT NULL AFTER `reschedule_count`,
  ADD COLUMN `last_rescheduled_at` DATETIME DEFAULT NULL AFTER `original_date`,
  ADD COLUMN `rescheduled_by`    ENUM('PATIENT','DOCTOR','ADMIN') DEFAULT NULL
      AFTER `last_rescheduled_at`,
  ADD CONSTRAINT `chk_reschedule_count` CHECK (`reschedule_count` BETWEEN 0 AND 10);

-- -----------------------------------------------------------------------------
-- Backfill earnings for consultations already completed, so the ledger is not
-- silently missing history the moment it is introduced.
-- 20% is the default commission also seeded below.
-- -----------------------------------------------------------------------------
INSERT INTO `doctor_earning`
  (`doctor_id`, `appointment_id`, `gross_amount`, `commission_rate`,
   `commission_amount`, `net_amount`, `status`, `earned_at`)
SELECT a.doctor_id,
       a.appointment_id,
       COALESCE(a.booked_fee, 0),
       20.00,
       ROUND(COALESCE(a.booked_fee, 0) * 0.20, 2),
       COALESCE(a.booked_fee, 0) - ROUND(COALESCE(a.booked_fee, 0) * 0.20, 2),
       'PENDING',
       COALESCE(a.completed_at, a.appointment_date)
  FROM `appointment` a
 WHERE a.status = 'Completed'
   AND COALESCE(a.booked_fee, 0) > 0;

-- -----------------------------------------------------------------------------
-- Policy settings. Kept in the database so an admin can change commercial terms
-- without a redeploy - the same reasoning as the pricing settings in V4.
-- -----------------------------------------------------------------------------
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  ('doctor_commission_percent',  '20', 'INT'),
  ('payout_cycle_days',          '15', 'INT'),
  ('reschedule_min_hours',       '4',  'INT'),
  ('max_reschedules',            '2',  'INT')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
