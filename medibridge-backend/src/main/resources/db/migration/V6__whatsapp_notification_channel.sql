-- =============================================================================
-- V6 - WhatsApp / SMS notification channel
--
-- The notification table already carried a `channel` column, but only ever
-- held EMAIL. This adds WHATSAPP as a first-class channel and widens the
-- de-duplication key so the same event can go out over both email AND WhatsApp
-- without the two colliding on the "send once" constraint.
-- =============================================================================

-- Add WHATSAPP to the channel enum. Widening an ENUM is safe for existing rows.
ALTER TABLE `notification`
  MODIFY COLUMN `channel` ENUM('EMAIL','SMS','WHATSAPP','PUSH') NOT NULL DEFAULT 'EMAIL';

-- The old key was (type, entity_type, entity_id, recipient_id) — which would
-- treat the email and the WhatsApp copy of one confirmation as duplicates.
-- Adding `channel` makes the key wider (more permissive), so no existing row
-- can violate it, while still guaranteeing one send per channel per event.
ALTER TABLE `notification` DROP INDEX `uq_notification_once`;
ALTER TABLE `notification`
  ADD UNIQUE KEY `uq_notification_once`
    (`type`, `entity_type`, `entity_id`, `recipient_id`, `channel`);
