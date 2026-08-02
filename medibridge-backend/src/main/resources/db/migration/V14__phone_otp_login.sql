-- =============================================================================
-- V14  PHONE OTP LOGIN
--
-- A patient signs in with their mobile number and a six-digit code; an unknown
-- number becomes a new patient. Doctors and admins keep email + password -
-- a doctor account means a licence an admin verified, and handing one out to
-- whoever controls a SIM is not a thing this schema will allow.
--
-- Three changes make that possible, and one new table enforces the policy.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. A normalised lookup key, SEPARATE from `phone`.
--
-- `phone` is what the user typed and what every screen displays - the seeded
-- rows hold "+91 90000 11111", spaces and all. Collapsing the two columns would
-- reformat every existing row to prove a point about storage.
--
-- So `phone` stays as-is and `phone_e164` carries the one canonical form that
-- "+91 90000 11111", "9000011111" and "09000011111" all reduce to. The Java
-- side reduces to exactly the same string (common/util/PhoneNumbers.toE164) -
-- it is both the send target and the lookup key, and if those two ever disagree
-- the system texts a number it cannot resolve back to an account.
--
-- UNIQUE is the whole point: one number, one account. NULL is allowed and
-- MySQL permits any number of NULLs in a unique index, so a row we could not
-- normalise simply does not participate in phone login.
-- -----------------------------------------------------------------------------
ALTER TABLE `patient`
  ADD COLUMN `phone_e164` VARCHAR(16) NULL AFTER `phone`;

-- Backfill through the same rules as the Java normaliser: strip everything that
-- is not a digit or '+', and assume +91 when no country code is present.
-- Bounded to 8..16 characters - 16 is E.164's own maximum and the column width,
-- and anything shorter is not a number anyone can be texted at.
UPDATE `patient` p
JOIN (
  SELECT `patient_id`,
         CASE
           WHEN c.clean = ''          THEN NULL
           WHEN c.clean LIKE '+%'     THEN c.clean
           ELSE CONCAT('+91', REGEXP_REPLACE(c.clean, '^0+', ''))
         END AS e164
  FROM (
    SELECT `patient_id`,
           REGEXP_REPLACE(COALESCE(`phone`, ''), '[^0-9+]', '') AS clean
    FROM `patient`
  ) c
) n ON n.`patient_id` = p.`patient_id`
SET p.`phone_e164` = n.e164
WHERE n.e164 IS NOT NULL
  AND CHAR_LENGTH(n.e164) BETWEEN 8 AND 16;

-- Existing data has no unique constraint on `phone`, so two rows may well share
-- a number - fixture rows and demo accounts especially. Those are left NULL
-- rather than picked between: a wrong guess here would sign one patient in to
-- another patient's record, which is the worst outcome this migration could
-- produce. They can claim the number by verifying it, which is the only
-- evidence that settles it.
UPDATE `patient` p
JOIN (
  SELECT `phone_e164`
  FROM `patient`
  WHERE `phone_e164` IS NOT NULL
  GROUP BY `phone_e164`
  HAVING COUNT(*) > 1
) d ON p.`phone_e164` = d.`phone_e164`
SET p.`phone_e164` = NULL;

ALTER TABLE `patient`
  ADD UNIQUE KEY `uq_patient_phone_e164` (`phone_e164`);


-- -----------------------------------------------------------------------------
-- 2. Email becomes optional.
--
-- Phone-first signup has no email at the moment the account is created - the
-- patient supplies one when they complete their profile. This is the same
-- "signed up without a password, profile filled in later" state V3 already
-- created for Google users, not a second one.
--
-- The UNIQUE index stays exactly as it was. MySQL allows any number of NULLs in
-- a unique index, so dropping it would give up a real guarantee for nothing.
-- -----------------------------------------------------------------------------
ALTER TABLE `patient`
  MODIFY COLUMN `email` VARCHAR(100) NULL;

ALTER TABLE `patient`
  MODIFY COLUMN `auth_provider` ENUM('LOCAL','GOOGLE','PHONE') NOT NULL DEFAULT 'LOCAL';


-- -----------------------------------------------------------------------------
-- 3. The codes themselves.
--
-- Expiry, the attempt count and single-use are COLUMNS checked against the row,
-- not a HashMap in the service. Same reasoning as uq_follow_up_parent: a counter
-- only the application remembers is a counter a restart forgets, and a second
-- instance never knew about. An attacker who can make the process recycle would
-- otherwise get unlimited guesses at a six-digit number.
--
-- `code_hash` rather than the code. Six digits do not survive a determined
-- offline attack on a SHA-256 hash and that is not the claim - ttl, the attempt
-- cap and single use are what make guessing pointless. Hashing keeps live login
-- codes out of backups, replicas and anything that dumps a table into a log.
--
-- created_at is load-bearing, not audit: the resend cooldown is derived from the
-- newest row for a number. AuthRateLimitFilter is per-IP and in memory, so it
-- cannot stop someone rotating addresses to make us send - and every send costs
-- real money at the gateway. A DB fact can stop them.
-- -----------------------------------------------------------------------------
CREATE TABLE `phone_otp` (
  `otp_id`      BIGINT AUTO_INCREMENT PRIMARY KEY,
  `phone_e164`  VARCHAR(16)  NOT NULL,
  `code_hash`   VARCHAR(64)  NOT NULL,
  `expires_at`  DATETIME     NOT NULL,
  `attempts`    INT          NOT NULL DEFAULT 0,
  `consumed_at` DATETIME     NULL DEFAULT NULL,
  `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- No FK to `patient`: a code is issued to a NUMBER, and the whole point of
  -- auto-registration is that the account does not exist yet.
  INDEX `idx_phone_otp_latest` (`phone_e164`, `otp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------------------------------
-- Policy knobs are deliberately NOT seeded, for the reason V13 gives: the
-- deployment default lives in application.yml (medibridge.otp.*) and
-- SettingsProvider passes it as the fallback, so a system_settings row is only
-- ever needed to OVERRIDE it from the admin screen.
--   otp_length, otp_ttl_minutes, otp_max_attempts, otp_resend_cooldown_seconds
-- -----------------------------------------------------------------------------
