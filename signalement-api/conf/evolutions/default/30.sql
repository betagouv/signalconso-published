-- !Ups

ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE(email);

-- !Downs

ALTER TABLE users DROP CONSTRAINT email_unique;
