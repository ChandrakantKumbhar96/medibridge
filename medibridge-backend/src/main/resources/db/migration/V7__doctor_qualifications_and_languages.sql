-- Richer doctor profiles: qualifications (e.g. "MBBS, MD, DM (Cardiology)") and
-- spoken languages, the way real telemedicine apps present a doctor. Nullable so
-- existing rows are valid; SampleDataSeeder backfills them in Java (it holds the
-- specialization objects, so the backfill lives with the rest of the demo data).
ALTER TABLE doctor
    ADD COLUMN qualifications VARCHAR(255) NULL,
    ADD COLUMN languages      VARCHAR(150) NULL;
