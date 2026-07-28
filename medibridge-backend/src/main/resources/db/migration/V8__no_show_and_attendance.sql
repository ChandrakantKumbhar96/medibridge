-- =============================================================================
-- V8  No-show outcome and consultation attendance
--
-- A paid appointment that nobody attended is neither Completed nor Cancelled.
-- It is its own outcome with its own money rule - the patient forfeits, the
-- doctor is still paid - so it needs its own status rather than being folded
-- into Cancelled, which would wrongly imply a refund.
--
-- Deciding *who* failed to attend is only defensible if attendance is recorded,
-- so the join timestamps live here too. They are written by the existing
-- GET /appointments/{id}/join endpoint, which is the only way either party can
-- obtain the room URL - there is no way to attend without passing through it.
-- =============================================================================

ALTER TABLE `appointment`
  MODIFY COLUMN `status` ENUM('PendingPayment','Requested','Accepted','Rejected',
                              'Rescheduled','Completed','Cancelled','AutoExpired',
                              'NoShow')
                DEFAULT 'PendingPayment';

ALTER TABLE `appointment`
  ADD COLUMN `patient_joined_at` DATETIME DEFAULT NULL AFTER `rescheduled_by`,
  ADD COLUMN `doctor_joined_at`  DATETIME DEFAULT NULL AFTER `patient_joined_at`,

  -- BOTH is a real outcome, not a placeholder: if neither side opened the room
  -- the platform cannot show the doctor was ready either, so the patient is
  -- refunded. Blaming the patient by default would be charging them for an
  -- absence we never proved.
  ADD COLUMN `no_show_by` ENUM('PATIENT','DOCTOR','BOTH') DEFAULT NULL
      AFTER `doctor_joined_at`,

  ADD COLUMN `no_show_marked_at` DATETIME DEFAULT NULL AFTER `no_show_by`;

-- The sweeper runs every few minutes and filters on (status, appointment_date).
-- idx_appt_date alone makes it scan every appointment ever booked in any state.
ALTER TABLE `appointment`
  ADD INDEX `idx_appt_status_date` (`status`, `appointment_date`);
