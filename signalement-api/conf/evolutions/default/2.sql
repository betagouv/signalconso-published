# --- !Ups

-- New column date_creation
ALTER TABLE SIGNALEMENT ADD COLUMN date_creation TIMESTAMP;
UPDATE SIGNALEMENT SET date_creation = now() WHERE date_creation IS NULL;
ALTER TABLE SIGNALEMENT ALTER COLUMN date_creation SET DEFAULT now();
ALTER TABLE SIGNALEMENT ALTER COLUMN date_creation SET NOT NULL;

-- New column date_constat
ALTER TABLE SIGNALEMENT ADD COLUMN date_constat DATE;
UPDATE SIGNALEMENT SET date_constat = now() WHERE date_constat IS NULL;
ALTER TABLE SIGNALEMENT ALTER COLUMN date_constat SET NOT NULL;

-- New column heure_constat
ALTER TABLE SIGNALEMENT ADD COLUMN heure_constat NUMERIC;

-- Rename column photo to anomalie_file_id
ALTER TABLE SIGNALEMENT RENAME COLUMN photo TO anomalie_file_id;

-- New column ticket_file_id
ALTER TABLE SIGNALEMENT ADD COLUMN ticket_file_id OID;

-- New column accord_contact
ALTER TABLE SIGNALEMENT ADD COLUMN accord_contact BOOLEAN;
UPDATE SIGNALEMENT SET accord_contact = '0' WHERE accord_contact IS NULL;
ALTER TABLE SIGNALEMENT ALTER COLUMN accord_contact SET NOT NULL;

# --- !Downs

ALTER TABLE SIGNALEMENT DROP date_creation;

ALTER TABLE SIGNALEMENT DROP date_constat;

ALTER TABLE SIGNALEMENT DROP heure_constat;

ALTER TABLE SIGNALEMENT RENAME COLUMN anomalie_file_id TO photo;

ALTER TABLE SIGNALEMENT DROP ticket_file_id;

ALTER TABLE SIGNALEMENT DROP accord_contact;