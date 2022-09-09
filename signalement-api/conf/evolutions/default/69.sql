-- !Ups

ALTER TABLE auth_attempts ADD COLUMN is_success BOOLEAN;
ALTER TABLE auth_attempts ADD COLUMN failure_cause VARCHAR;

-- !Downs

ALTER TABLE auth_attempts DROP COLUMN is_success;
ALTER TABLE auth_attempts DROP COLUMN failure_cause;