-- !Ups

ALTER TABLE report_files ADD COLUMN storage_filename VARCHAR;
UPDATE report_files SET storage_filename = id WHERE storage_filename IS NULL;
ALTER TABLE report_files ALTER COLUMN storage_filename SET NOT NULL;

-- !Downs

ALTER TABLE report_files DROP COLUMN storage_filename;
