-- !Ups

ALTER TABLE users ADD COLUMN last_email_validation timestamptz;
UPDATE users SET last_email_validation = NOW() WHERE last_email_validation IS NULL;

-- !Downs

ALTER TABLE users DROP COLUMN last_email_validation;
