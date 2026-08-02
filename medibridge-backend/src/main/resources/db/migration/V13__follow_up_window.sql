-- =============================================================================
-- V13  FREE FOLLOW-UP WINDOW
--
-- After a consultation is completed the patient may book one revisit with the
-- same doctor, within a configured number of days, at no charge. Clinics have
-- always worked this way; the schema simply had no way to say that one booking
-- descends from another.
--
-- The link is a self-reference on `appointment`, and the UNIQUE index on it is
-- the whole guarantee: one completed visit yields AT MOST ONE free follow-up.
-- A boolean `is_follow_up_used` on the parent would have been the other option
-- and is the wrong one - it is a cached fact that Java has to remember to set,
-- which is exactly the mistake doctor_schedule.is_booked already makes. Here
-- the fact IS the row, and two concurrent bookings off the same parent lose at
-- the index rather than at a check that raced.
--
-- MySQL allows any number of NULLs in a UNIQUE index, so ordinary bookings -
-- which are the overwhelming majority - are unaffected.
-- =============================================================================

ALTER TABLE `appointment`
  ADD COLUMN `parent_appointment_id` INT DEFAULT NULL AFTER `schedule_id`,

  -- RESTRICT, not SET NULL: silently unlinking a follow-up would give its
  -- parent a second free revisit that policy never granted.
  ADD CONSTRAINT `fk_appt_parent`
      FOREIGN KEY (`parent_appointment_id`) REFERENCES `appointment`(`appointment_id`)
      ON DELETE RESTRICT,

  ADD UNIQUE KEY `uq_follow_up_parent` (`parent_appointment_id`);


-- -----------------------------------------------------------------------------
-- Policy knobs.
--
-- Deliberately NOT seeded here. The deployment default lives in application.yml
-- (medibridge.follow-up.*) and SettingsProvider passes it as the fallback, so a
-- row is only ever needed to OVERRIDE that default from the admin screen -
-- seeding one would freeze today's yml value into every database and make the
-- config file dead weight.
-- -----------------------------------------------------------------------------
