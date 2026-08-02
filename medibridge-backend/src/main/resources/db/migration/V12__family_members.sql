-- =============================================================================
-- V12  FAMILY / DEPENDENT PROFILES
--
-- A parent booking for a child is the most common real booking the schema could
-- not express. A dependent is NOT a row in `patient`: they have no email, no
-- password_hash and no way to sign in, so putting them there would mean making
-- the login columns nullable and weakening the UNIQUE(email) that authentication
-- relies on. They get their own table, owned by exactly one patient.
-- =============================================================================

CREATE TABLE `family_member` (
  `family_member_id` INT AUTO_INCREMENT PRIMARY KEY,
  `patient_id`       INT          NOT NULL,
  `full_name`        VARCHAR(100) NOT NULL,
  `date_of_birth`    DATE         NOT NULL,
  `gender`           ENUM('Male','Female','Other') NOT NULL,
  `relation`         ENUM('Child','Spouse','Parent','Sibling','Other') NOT NULL,
  `blood_group`      VARCHAR(5)   DEFAULT NULL,
  `phone`            VARCHAR(20)  DEFAULT NULL,

  -- Soft delete. A dependent with appointments, reports or prescriptions behind
  -- them cannot be hard-deleted without destroying clinical history, and the
  -- foreign keys below would refuse it anyway. Archiving hides them from the
  -- booking dropdown while their records stay readable.
  `archived_at`      DATETIME     DEFAULT NULL,

  `created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (`patient_id`) REFERENCES `patient`(`patient_id`) ON DELETE CASCADE,
  INDEX `idx_family_owner` (`patient_id`, `archived_at`),

  -- Not redundant with the PK: it is the index the composite foreign keys below
  -- point at, and it is what makes "this dependent belongs to this patient" a
  -- referenceable fact rather than something only Java knows.
  UNIQUE KEY `uq_family_member_owner` (`family_member_id`, `patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------------------------------
-- APPOINTMENT  ->  who the visit is actually for
--
-- NULL means the account holder themselves. The foreign key is deliberately
-- COMPOSITE: (family_member_id, patient_id) must exist together in
-- family_member, so the database itself refuses an appointment that pairs
-- patient A with patient B's dependent. The service layer checks this too, but
-- only so the failure reads as a clean 404 instead of a constraint violation -
-- the guarantee lives here, the same way uq_schedule_booking, not
-- doctor_schedule.is_booked, is what actually prevents double booking.
--
-- MySQL's MATCH SIMPLE semantics mean a NULL family_member_id satisfies the
-- constraint without a lookup, so self-bookings are unaffected.
-- -----------------------------------------------------------------------------
ALTER TABLE `appointment`
  ADD COLUMN `family_member_id` INT DEFAULT NULL AFTER `patient_id`,
  ADD CONSTRAINT `fk_appt_family_member`
      FOREIGN KEY (`family_member_id`, `patient_id`)
      REFERENCES `family_member`(`family_member_id`, `patient_id`)
      ON DELETE RESTRICT,
  ADD INDEX `idx_appt_family` (`family_member_id`);


-- -----------------------------------------------------------------------------
-- MEDICAL_REPORT  ->  whose document this is
--
-- Reports are uploaded outside any appointment, so unlike a prescription this
-- one cannot derive its subject from a visit - it needs its own column.
-- -----------------------------------------------------------------------------
ALTER TABLE `medical_report`
  ADD COLUMN `family_member_id` INT DEFAULT NULL AFTER `patient_id`,
  ADD CONSTRAINT `fk_report_family_member`
      FOREIGN KEY (`family_member_id`, `patient_id`)
      REFERENCES `family_member`(`family_member_id`, `patient_id`)
      ON DELETE RESTRICT,
  ADD INDEX `idx_report_family` (`family_member_id`, `upload_date`);


-- A prescription deliberately gets NO column here. Its subject is whoever the
-- consultation was for, which the appointment already records; copying it would
-- be a second source of truth that can drift - the same reason V1 dropped
-- consultation_record.has_prescription.
