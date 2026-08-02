-- =============================================================================
-- V11  Second opinion as a real consultation type
--
-- Until now "Second Opinion" was a string in appointment.consult_type and
-- nothing else: the flow ended in the ordinary prescription screen, so what the
-- patient actually received was a prescription. That is not what a second
-- opinion is. Its whole value is a written judgement on someone else's
-- diagnosis - whether the reviewing specialist agrees, and what they would do
-- instead - and none of that fits a medicine table.
--
-- So it gets its own document. Deliberately NOT a variant of `prescription`:
-- a second opinion may legitimately prescribe nothing at all, and forcing it
-- through a table whose reason to exist is a list of medicines would make the
-- empty case look like an incomplete record.
-- =============================================================================

CREATE TABLE `medical_opinion` (
  `opinion_id`             INT AUTO_INCREMENT PRIMARY KEY,

  -- UNIQUE, not just FK: one appointment yields one opinion. A second row would
  -- be a second document for one consultation, with no way to tell which the
  -- patient should act on.
  `appointment_id`         INT NOT NULL UNIQUE,

  -- What the patient came with. Recorded separately from the reviewer's own
  -- findings so the document shows what was actually being reviewed - without
  -- it, a disagreement months later cannot be read back.
  `original_diagnosis`     TEXT         NOT NULL,

  `findings`               TEXT         NOT NULL,

  -- The answer the patient is paying for, as a column rather than buried in
  -- prose: it is the one field that can be listed, filtered and shown as a
  -- verdict on screen.
  `agrees_with_original`   BOOLEAN      NOT NULL,

  `recommendation`         TEXT         NOT NULL,

  -- Optional: many second opinions end in "no change needed", which is a
  -- complete answer and must not read as a missing one.
  `suggested_tests`        TEXT         DEFAULT NULL,

  `issued_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at`             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (`appointment_id`) REFERENCES `appointment`(`appointment_id`)
      ON DELETE CASCADE,

  INDEX `idx_opinion_issued` (`issued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Pricing. A second opinion is more work than a first consultation - the
-- specialist reads someone else's case file before they say anything - so it
-- is not billed at the same rate. Stored as a percentage of the doctor's normal
-- fee rather than a flat amount, so it scales with each doctor's own pricing.
--
-- INT because SettingsProvider.getInt is the reader; 150 means 150%.
-- -----------------------------------------------------------------------------
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  ('second_opinion_fee_percent', '150', 'INT'),

  -- A second opinion with nothing to review is not a second opinion. This is the
  -- minimum number of documents a patient must have on file before they can book
  -- one; 0 disables the requirement.
  ('second_opinion_min_reports', '1', 'INT')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
