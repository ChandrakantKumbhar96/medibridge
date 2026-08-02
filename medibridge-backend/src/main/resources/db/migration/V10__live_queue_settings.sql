-- =============================================================================
-- V10  Live queue thresholds
--
-- No schema change: the queue is derived entirely from data V8 already records
-- (patient_joined_at / doctor_joined_at) against appointment_date. Storing a
-- position or an ETA would be storing a number that is wrong seconds later -
-- every booking, cancellation and join in the day moves it.
--
-- Only the two thresholds that shape the estimate are persisted, and they are
-- settings rather than constants for the usual reason: what counts as "running
-- late" is a commercial judgement an admin should be able to tune without a
-- redeploy. SettingsProvider carries the same values as fallbacks, so a missing
-- row degrades to the default instead of breaking the queue.
-- =============================================================================

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  -- Under 5 minutes behind is on time. Announcing a 3-minute delay is noise,
  -- and it invites the patient to treat every estimate as unreliable.
  ('queue_min_delay_minutes', '5',   'INT'),

  -- Cap. Past two hours the figure has stopped being an estimate of a wait and
  -- become evidence that something is stuck.
  ('queue_max_delay_minutes', '120', 'INT')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
