-- !Ups

ALTER TABLE companies ADD COLUMN activity_code VARCHAR;

-- !Downs

ALTER TABLE companies DROP COLUMN activity_code;
