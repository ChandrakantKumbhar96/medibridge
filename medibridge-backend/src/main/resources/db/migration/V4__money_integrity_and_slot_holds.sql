-- =============================================================================
-- V4 - Money integrity, slot holds, meeting windows, notifications
--
-- Fixes four logical flaws:
--   1. the UI showed fee + platform fee but only the fee was charged
--   2. the fee was read at payment time, so a doctor's price change could
--      alter what an already-booked patient paid
--   3. abandoned checkouts held a slot forever
--   4. a refund flipped a status without any gateway call
-- =============================================================================

-- -----------------------------------------------------------------------------
-- APPOINTMENT: price is agreed at booking, not looked up later.
--
-- Snapshotting is the industry norm: an invoice must remain reproducible even
-- after the doctor changes their rate, and the patient must be charged exactly
-- what they were quoted.
-- -----------------------------------------------------------------------------
ALTER TABLE `appointment`
  ADD COLUMN `booked_fee`    DECIMAL(10,2) NULL AFTER `consult_type`,
  ADD COLUMN `platform_fee`  DECIMAL(10,2) NULL AFTER `booked_fee`,
  ADD COLUMN `total_amount`  DECIMAL(10,2) NULL AFTER `platform_fee`,

  -- A PENDING_PAYMENT appointment holds its slot only until this instant.
  -- A scheduled job expires the row and frees the slot afterwards.
  ADD COLUMN `hold_expires_at` DATETIME NULL AFTER `total_amount`,

  -- The consultation link is only usable inside this window. A link that works
  -- forever is a link anyone it was forwarded to can walk into.
  ADD COLUMN `meeting_join_from`   DATETIME NULL AFTER `meeting_sent_at`,
  ADD COLUMN `meeting_valid_until` DATETIME NULL AFTER `meeting_join_from`,

  ADD COLUMN `confirmed_at`        DATETIME NULL AFTER `meeting_valid_until`,
  ADD COLUMN `completed_at`        DATETIME NULL AFTER `confirmed_at`,
  ADD COLUMN `cancelled_at`        DATETIME NULL AFTER `completed_at`,
  ADD COLUMN `cancelled_by`        ENUM('PATIENT','DOCTOR','ADMIN','SYSTEM') NULL AFTER `cancelled_at`,
  ADD COLUMN `cancellation_reason` VARCHAR(255) NULL AFTER `cancelled_by`,

  ADD INDEX `idx_appt_hold_expiry` (`status`, `hold_expires_at`);

-- Backfill existing rows so the new columns are never silently null in reports.
UPDATE `appointment` a
  JOIN `doctor` d ON d.doctor_id = a.doctor_id
   SET a.booked_fee   = d.consultation_fee,
       a.platform_fee = 0.00,
       a.total_amount = d.consultation_fee
 WHERE a.booked_fee IS NULL;

-- -----------------------------------------------------------------------------
-- PAYMENT: record the split, and track real gateway refunds.
-- -----------------------------------------------------------------------------
ALTER TABLE `payment_transaction`
  ADD COLUMN `consultation_amount` DECIMAL(10,2) NULL AFTER `amount`,
  ADD COLUMN `platform_fee`        DECIMAL(10,2) NULL AFTER `consultation_amount`,
  ADD COLUMN `gateway_refund_id`   VARCHAR(100)  NULL AFTER `refunded_at`,
  ADD COLUMN `refund_reason`       VARCHAR(255)  NULL AFTER `gateway_refund_id`,
  ADD INDEX `idx_txn_refund` (`gateway_refund_id`);

UPDATE `payment_transaction`
   SET `consultation_amount` = `amount`, `platform_fee` = 0.00
 WHERE `consultation_amount` IS NULL;

-- -----------------------------------------------------------------------------
-- NOTIFICATION: reminders need a record of what was already sent, otherwise a
-- job that runs twice mails the patient twice.
-- -----------------------------------------------------------------------------
CREATE TABLE `notification` (
  `notification_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `recipient_type`  ENUM('PATIENT','DOCTOR','ADMIN') NOT NULL,
  `recipient_id`    VARCHAR(36)  NOT NULL,
  `recipient_email` VARCHAR(100) NOT NULL,
  `channel`         ENUM('EMAIL','SMS','PUSH') NOT NULL DEFAULT 'EMAIL',
  `type`            VARCHAR(50)  NOT NULL,
  `subject`         VARCHAR(200) NOT NULL,
  `body`            TEXT,
  `status`          ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
  `error_message`   VARCHAR(255) NULL,
  `entity_type`     VARCHAR(50)  NULL,
  `entity_id`       VARCHAR(36)  NULL,
  `sent_at`         DATETIME     NULL,
  `created_at`      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  -- Stops a re-run of the reminder job sending the same notice twice.
  UNIQUE KEY `uq_notification_once` (`type`, `entity_type`, `entity_id`, `recipient_id`),
  INDEX `idx_notification_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Data-integrity constraints that were missing.
-- MySQL 8.0.16+ enforces CHECK rather than parsing and ignoring it.
-- -----------------------------------------------------------------------------
ALTER TABLE `doctor`
  ADD CONSTRAINT `chk_doctor_fee`        CHECK (`consultation_fee` >= 0),
  ADD CONSTRAINT `chk_doctor_experience` CHECK (`experience_years` BETWEEN 0 AND 70),
  ADD CONSTRAINT `chk_doctor_duration`   CHECK (`consultation_duration_min` BETWEEN 5 AND 240),
  ADD CONSTRAINT `chk_doctor_rating`     CHECK (`rating_avg` BETWEEN 0 AND 5);

ALTER TABLE `appointment`
  ADD CONSTRAINT `chk_appt_amounts` CHECK (
        (`booked_fee`   IS NULL OR `booked_fee`   >= 0)
    AND (`platform_fee` IS NULL OR `platform_fee` >= 0)
    AND (`total_amount` IS NULL OR `total_amount` >= 0));

ALTER TABLE `payment_transaction`
  ADD CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0),
  ADD CONSTRAINT `chk_refund_not_more_than_paid`
      CHECK (`refund_amount` IS NULL OR `refund_amount` <= `amount`);

ALTER TABLE `doctor_schedule`
  ADD CONSTRAINT `chk_slot_times` CHECK (`end_time` > `start_time`);

-- -----------------------------------------------------------------------------
-- Pricing and policy settings, so business rules are configurable rather than
-- compiled into the code.
-- -----------------------------------------------------------------------------
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  ('platform_fee',              '5',  'INT'),
  ('slot_hold_minutes',         '15', 'INT'),
  ('free_cancellation_hours',   '24', 'INT'),
  ('partial_refund_percent',    '50', 'INT'),
  ('meeting_join_before_min',   '15', 'INT'),
  ('meeting_valid_after_min',   '60', 'INT'),
  ('reminder_hours_before',     '24', 'INT')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
