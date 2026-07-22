-- =============================================================================
-- V3 - Google Sign-In + real payment gateway support
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Google Sign-In
--
-- A Google user never sets a password here, so password_hash must become
-- nullable. `auth_provider` is what makes that safe: the login path refuses a
-- password login for a GOOGLE account rather than comparing against NULL.
--
-- google_sub is Google's stable subject id. Matching on it rather than on email
-- matters - a Google account's email can change, the sub never does.
-- -----------------------------------------------------------------------------
ALTER TABLE `patient`
  MODIFY COLUMN `password_hash` VARCHAR(255) NULL,
  ADD COLUMN `auth_provider` ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL' AFTER `password_hash`,
  ADD COLUMN `google_sub` VARCHAR(64) NULL AFTER `auth_provider`,
  ADD COLUMN `avatar_url` VARCHAR(500) NULL AFTER `google_sub`,
  ADD UNIQUE KEY `uq_patient_google_sub` (`google_sub`);

-- Google users have no date_of_birth / gender / blood_group until they complete
-- their profile, so these can no longer be required at row creation.
ALTER TABLE `patient`
  MODIFY COLUMN `date_of_birth` DATE NULL,
  MODIFY COLUMN `gender` ENUM('Male','Female','Other') NULL,
  MODIFY COLUMN `blood_group` VARCHAR(5) NULL,
  MODIFY COLUMN `phone` VARCHAR(20) NULL;

-- -----------------------------------------------------------------------------
-- Payment gateway
--
-- gateway_order_id is created server-side before checkout opens; the payment id
-- and signature come back from the gateway afterwards. The signature is stored
-- so a disputed payment can be re-verified later.
--
-- `gateway` distinguishes a real gateway payment from the simulated path, which
-- stays available so the app still runs without API keys.
-- -----------------------------------------------------------------------------
ALTER TABLE `payment_transaction`
  ADD COLUMN `gateway` ENUM('SIMULATED','RAZORPAY') NOT NULL DEFAULT 'SIMULATED' AFTER `payment_method`,
  ADD COLUMN `gateway_order_id` VARCHAR(100) NULL AFTER `gateway`,
  ADD COLUMN `gateway_payment_id` VARCHAR(100) NULL AFTER `gateway_order_id`,
  ADD COLUMN `gateway_signature` VARCHAR(255) NULL AFTER `gateway_payment_id`,
  ADD COLUMN `failure_reason` VARCHAR(255) NULL AFTER `gateway_signature`,
  ADD UNIQUE KEY `uq_gateway_order` (`gateway_order_id`),
  ADD INDEX `idx_gateway_payment` (`gateway_payment_id`);
