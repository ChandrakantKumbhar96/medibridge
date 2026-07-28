-- =============================================================================
-- V9  Drug identity and interaction checking
--
-- prescription_item already stores structured medicines, but `medicine_name` is
-- free text: 'Amoxicillin', 'amoxycillin' and 'Amoxil' are three unrelated
-- strings. Nothing can reason across prescriptions until a medicine has an
-- identity, so this adds one - plus the interaction data that identity makes
-- usable, and a computable end date so "is the patient still on this?" is a
-- query rather than a guess.
--
-- SAFETY NOTE, and it belongs in the schema rather than only in the docs:
-- the interaction rows below are a small teaching dataset of well-established
-- pairs. They are NOT a clinical reference and are nowhere near exhaustive.
-- The feature exists to surface a second opinion to a qualified prescriber who
-- remains responsible for the decision - never to approve a prescription, and
-- never to be shown to a patient as clinical advice. Silence from this table
-- means "no match in our data", not "safe".
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. DRUG  - canonical identity for a medicine
--    `generic_name` is what interactions are really about; two brands of the
--    same molecule interact identically, so the checker resolves to generic.
-- -----------------------------------------------------------------------------
CREATE TABLE `drug` (
  `drug_id`      INT AUTO_INCREMENT PRIMARY KEY,
  `name`         VARCHAR(150) NOT NULL UNIQUE,
  `generic_name` VARCHAR(150) NOT NULL,
  `drug_class`   VARCHAR(100) DEFAULT NULL,
  `created_at`   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_drug_generic` (`generic_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. DRUG_ALIAS  - brand names and common misspellings
--    Kept out of `drug` so one molecule can have many spellings without
--    duplicating the identity row. This is what makes autocomplete forgiving.
-- -----------------------------------------------------------------------------
CREATE TABLE `drug_alias` (
  `alias_id` INT AUTO_INCREMENT PRIMARY KEY,
  `drug_id`  INT          NOT NULL,
  `alias`    VARCHAR(150) NOT NULL,
  UNIQUE KEY `uq_drug_alias` (`alias`),
  FOREIGN KEY (`drug_id`) REFERENCES `drug`(`drug_id`) ON DELETE CASCADE,
  INDEX `idx_alias_drug` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. DRUG_INTERACTION  - unordered pairs, stored once
--    CHECK (drug_a_id < drug_b_id) is what makes the pair unordered: without
--    it (A,B) and (B,A) are two rows, the UNIQUE key allows both, and a lookup
--    has to query in both directions and hope neither is missing. Normalising
--    the order on write turns that into one row and one lookup.
-- -----------------------------------------------------------------------------
CREATE TABLE `drug_interaction` (
  `interaction_id` INT AUTO_INCREMENT PRIMARY KEY,
  `drug_a_id`      INT NOT NULL,
  `drug_b_id`      INT NOT NULL,
  `severity`       ENUM('Minor','Moderate','Severe') NOT NULL,
  `description`    VARCHAR(400) NOT NULL,
  `created_at`     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_interaction_pair` (`drug_a_id`, `drug_b_id`),
  CONSTRAINT `chk_interaction_order` CHECK (`drug_a_id` < `drug_b_id`),
  FOREIGN KEY (`drug_a_id`) REFERENCES `drug`(`drug_id`) ON DELETE CASCADE,
  FOREIGN KEY (`drug_b_id`) REFERENCES `drug`(`drug_id`) ON DELETE CASCADE,
  INDEX `idx_interaction_b` (`drug_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. PRESCRIPTION_ITEM  - identity and a computable course end
--    drug_id is NULLABLE on purpose. A doctor must never be blocked from
--    prescribing something the reference data has not heard of, and every row
--    written before this migration has no identity to backfill. An unresolved
--    item is simply invisible to the checker, which is the honest behaviour.
--
--    duration_days exists because `duration` is a display string ('5 days',
--    '2 weeks', 'till review'). "Is the patient still taking this?" cannot be
--    answered by parsing that at query time, and answering it wrongly is how a
--    checker either misses a live interaction or warns about a finished course.
-- -----------------------------------------------------------------------------
ALTER TABLE `prescription_item`
  ADD COLUMN `drug_id`       INT DEFAULT NULL AFTER `medicine_name`,
  ADD COLUMN `duration_days` INT DEFAULT NULL AFTER `duration`,
  ADD CONSTRAINT `fk_item_drug` FOREIGN KEY (`drug_id`)
      REFERENCES `drug`(`drug_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `chk_item_duration_days`
      CHECK (`duration_days` IS NULL OR `duration_days` BETWEEN 1 AND 3650);

-- Backs the cross-doctor lookup: "every identified medicine for this patient".
ALTER TABLE `prescription_item`
  ADD INDEX `idx_item_drug` (`drug_id`);

-- -----------------------------------------------------------------------------
-- 5. REFERENCE DATA
--    A deliberately small, common set - enough to demonstrate the mechanism
--    honestly rather than pretend to be a formulary.
-- -----------------------------------------------------------------------------
INSERT INTO `drug` (`name`, `generic_name`, `drug_class`) VALUES
  ('Paracetamol',     'paracetamol',     'Analgesic / antipyretic'),
  ('Ibuprofen',       'ibuprofen',       'NSAID'),
  ('Diclofenac',      'diclofenac',      'NSAID'),
  ('Aspirin',         'aspirin',         'Antiplatelet / NSAID'),
  ('Naproxen',        'naproxen',        'NSAID'),
  ('Warfarin',        'warfarin',        'Anticoagulant'),
  ('Clopidogrel',     'clopidogrel',     'Antiplatelet'),
  ('Omeprazole',      'omeprazole',      'Proton pump inhibitor'),
  ('Pantoprazole',    'pantoprazole',    'Proton pump inhibitor'),
  ('Amoxicillin',     'amoxicillin',     'Penicillin antibiotic'),
  ('Azithromycin',    'azithromycin',    'Macrolide antibiotic'),
  ('Clarithromycin',  'clarithromycin',  'Macrolide antibiotic'),
  ('Ciprofloxacin',   'ciprofloxacin',   'Fluoroquinolone antibiotic'),
  ('Metronidazole',   'metronidazole',   'Antibiotic / antiprotozoal'),
  ('Simvastatin',     'simvastatin',     'Statin'),
  ('Atorvastatin',    'atorvastatin',    'Statin'),
  ('Amlodipine',      'amlodipine',      'Calcium channel blocker'),
  ('Lisinopril',      'lisinopril',      'ACE inhibitor'),
  ('Telmisartan',     'telmisartan',     'ARB'),
  ('Spironolactone',  'spironolactone',  'Potassium-sparing diuretic'),
  ('Furosemide',      'furosemide',      'Loop diuretic'),
  ('Digoxin',         'digoxin',         'Cardiac glycoside'),
  ('Metformin',       'metformin',       'Biguanide antidiabetic'),
  ('Glimepiride',     'glimepiride',     'Sulfonylurea antidiabetic'),
  ('Levothyroxine',   'levothyroxine',   'Thyroid hormone'),
  ('Calcium Carbonate','calcium carbonate','Mineral supplement'),
  ('Sertraline',      'sertraline',      'SSRI antidepressant'),
  ('Fluoxetine',      'fluoxetine',      'SSRI antidepressant'),
  ('Tramadol',        'tramadol',        'Opioid analgesic'),
  ('Cetirizine',      'cetirizine',      'Antihistamine'),
  ('Montelukast',     'montelukast',     'Leukotriene antagonist'),
  ('Salbutamol',      'salbutamol',      'Bronchodilator'),
  ('Prednisolone',    'prednisolone',    'Corticosteroid'),
  ('Ranitidine',      'ranitidine',      'H2 blocker');

-- Brand names and spellings a doctor is likely to actually type.
INSERT INTO `drug_alias` (`drug_id`, `alias`)
SELECT `drug_id`, `a` FROM `drug`
JOIN (
  SELECT 'Paracetamol' AS n, 'Crocin'      AS a UNION ALL
  SELECT 'Paracetamol',      'Dolo'              UNION ALL
  SELECT 'Paracetamol',      'Acetaminophen'     UNION ALL
  SELECT 'Ibuprofen',        'Brufen'            UNION ALL
  SELECT 'Diclofenac',       'Voveran'           UNION ALL
  SELECT 'Aspirin',          'Ecosprin'          UNION ALL
  SELECT 'Aspirin',          'Acetylsalicylic acid' UNION ALL
  SELECT 'Amoxicillin',      'Amoxil'            UNION ALL
  SELECT 'Amoxicillin',      'Amoxycillin'       UNION ALL
  SELECT 'Azithromycin',     'Azithral'          UNION ALL
  SELECT 'Ciprofloxacin',    'Ciplox'            UNION ALL
  SELECT 'Omeprazole',       'Omez'              UNION ALL
  SELECT 'Pantoprazole',     'Pantocid'          UNION ALL
  SELECT 'Atorvastatin',     'Atorva'            UNION ALL
  SELECT 'Amlodipine',       'Amlong'            UNION ALL
  SELECT 'Metformin',        'Glycomet'          UNION ALL
  SELECT 'Levothyroxine',    'Thyronorm'         UNION ALL
  SELECT 'Levothyroxine',    'Eltroxin'          UNION ALL
  SELECT 'Cetirizine',       'Cetzine'           UNION ALL
  SELECT 'Montelukast',      'Montair'           UNION ALL
  SELECT 'Salbutamol',       'Asthalin'          UNION ALL
  SELECT 'Salbutamol',       'Albuterol'         UNION ALL
  SELECT 'Prednisolone',     'Omnacortil'        UNION ALL
  SELECT 'Sertraline',       'Zoloft'            UNION ALL
  SELECT 'Tramadol',         'Ultracet'
) AS aliases ON aliases.n = `drug`.`name`;

-- Pairs are inserted through LEAST/GREATEST so the CHECK constraint holds
-- whichever order they are written in here.
INSERT INTO `drug_interaction` (`drug_a_id`, `drug_b_id`, `severity`, `description`)
SELECT LEAST(d1.`drug_id`, d2.`drug_id`),
       GREATEST(d1.`drug_id`, d2.`drug_id`),
       p.`sev`, p.`descr`
FROM (
  SELECT 'Warfarin' AS x, 'Aspirin' AS y, 'Severe' AS sev,
         'Both reduce clotting. Taken together the bleeding risk rises sharply, including gastrointestinal and intracranial bleeding.' AS descr UNION ALL
  SELECT 'Warfarin', 'Ibuprofen', 'Severe',
         'NSAIDs add antiplatelet action and gastric irritation on top of anticoagulation, substantially raising bleeding risk.' UNION ALL
  SELECT 'Warfarin', 'Diclofenac', 'Severe',
         'NSAIDs add antiplatelet action and gastric irritation on top of anticoagulation, substantially raising bleeding risk.' UNION ALL
  SELECT 'Warfarin', 'Naproxen', 'Severe',
         'NSAIDs add antiplatelet action and gastric irritation on top of anticoagulation, substantially raising bleeding risk.' UNION ALL
  SELECT 'Warfarin', 'Metronidazole', 'Severe',
         'Metronidazole inhibits warfarin metabolism, raising INR and bleeding risk.' UNION ALL
  SELECT 'Clarithromycin', 'Simvastatin', 'Severe',
         'Clarithromycin blocks simvastatin metabolism. Statin levels rise, risking myopathy and rhabdomyolysis.' UNION ALL
  SELECT 'Clopidogrel', 'Omeprazole', 'Moderate',
         'Omeprazole inhibits the enzyme that activates clopidogrel, which may reduce its antiplatelet effect. Pantoprazole is usually preferred.' UNION ALL
  SELECT 'Aspirin', 'Ibuprofen', 'Moderate',
         'Ibuprofen can block the antiplatelet effect of low-dose aspirin, and both raise gastrointestinal bleeding risk.' UNION ALL
  SELECT 'Lisinopril', 'Spironolactone', 'Severe',
         'Both raise serum potassium. Combined use can cause dangerous hyperkalaemia; potassium needs monitoring.' UNION ALL
  SELECT 'Telmisartan', 'Spironolactone', 'Severe',
         'Both raise serum potassium. Combined use can cause dangerous hyperkalaemia; potassium needs monitoring.' UNION ALL
  SELECT 'Lisinopril', 'Ibuprofen', 'Moderate',
         'NSAIDs blunt the blood-pressure effect of ACE inhibitors and, together, can impair kidney function.' UNION ALL
  SELECT 'Digoxin', 'Furosemide', 'Moderate',
         'Furosemide lowers potassium, which increases the risk of digoxin toxicity.' UNION ALL
  SELECT 'Sertraline', 'Tramadol', 'Severe',
         'Both raise serotonin levels. Combined use risks serotonin syndrome and lowers the seizure threshold.' UNION ALL
  SELECT 'Fluoxetine', 'Tramadol', 'Severe',
         'Both raise serotonin levels. Combined use risks serotonin syndrome and lowers the seizure threshold.' UNION ALL
  SELECT 'Sertraline', 'Aspirin', 'Moderate',
         'SSRIs impair platelet function; with aspirin the gastrointestinal bleeding risk increases.' UNION ALL
  SELECT 'Levothyroxine', 'Calcium Carbonate', 'Moderate',
         'Calcium binds levothyroxine in the gut and reduces its absorption. Doses should be separated by at least four hours.' UNION ALL
  SELECT 'Ciprofloxacin', 'Prednisolone', 'Moderate',
         'Corticosteroids with fluoroquinolones increase the risk of tendinitis and tendon rupture, especially in older patients.' UNION ALL
  SELECT 'Metformin', 'Furosemide', 'Minor',
         'Loop diuretics can raise metformin levels and affect renal function; monitoring is advised in renal impairment.' UNION ALL
  SELECT 'Glimepiride', 'Ciprofloxacin', 'Moderate',
         'Fluoroquinolones can potentiate sulfonylureas, causing unexpected hypoglycaemia.' UNION ALL
  SELECT 'Simvastatin', 'Amlodipine', 'Moderate',
         'Amlodipine raises simvastatin exposure; the simvastatin dose is usually capped at 20 mg when combined.'
) AS p
JOIN `drug` d1 ON d1.`name` = p.`x`
JOIN `drug` d2 ON d2.`name` = p.`y`;
