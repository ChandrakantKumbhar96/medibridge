-- =============================================================================
-- Reference data + a demo admin.
-- Specialization names match the dropdown in DoctorRegisterForm.jsx exactly.
-- =============================================================================

INSERT INTO `specialization` (`name`, `emoji`, `description`) VALUES
  ('Cardiology',        '❤️', 'Heart and cardiovascular care'),
  ('Dermatology',       '🔬', 'Skin, hair and nail conditions'),
  ('General Physician', '🩺', 'Primary care for all ages'),
  ('Orthopedics',       '🦴', 'Bones, joints and sports injuries'),
  ('Pediatrics',        '👶', 'Child and adolescent health'),
  ('Neurology',         '🧠', 'Brain and nervous system');

-- NOTE: the demo admin is seeded in Java (common/config/DataSeeder) rather than
-- here, so the BCrypt hash is produced by the same PasswordEncoder bean the
-- login path uses. A hand-written hash in SQL is impossible to verify and
-- silently breaks login if it is wrong.

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `value_type`) VALUES
  ('platform_name',            'MediBridge',             'STRING'),
  ('support_email',            'support@medibridge.com', 'STRING'),
  ('max_appointments_per_day', '50',                     'INT'),
  ('two_factor_enabled',       'true',                   'BOOLEAN'),
  ('session_timeout_minutes',  '30',                     'INT');
