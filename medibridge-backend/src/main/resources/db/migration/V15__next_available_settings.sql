-- =============================================================================
-- V15  "Next available" matching thresholds
--
-- No schema change - the feature reads doctor_schedule directly. Only the two
-- numbers that shape the offer are persisted, same reasoning as V10's queue
-- thresholds: how soon is "reachable" and how far ahead is "worth offering"
-- are commercial judgements an admin should be able to tune without a
-- redeploy, not constants buried in AppointmentService.
-- =============================================================================

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  -- A slot starting in 2 minutes is not bookable - the patient has not paid
  -- yet. This is the minimum lead time an offer must give them.
  ('next_available_floor_minutes', '15', 'INT'),

  -- How far ahead the search looks. Wider than "today" on purpose - a search
  -- run at 6pm should still be able to offer tomorrow 9am rather than coming
  -- back empty for the rest of the evening.
  ('next_available_window_hours',  '24', 'INT')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
