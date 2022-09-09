-- !Ups

ALTER TABLE reports ADD COLUMN consumer_phone VARCHAR;

-- !Downs

ALTER TABLE reports DROP COLUMN consumer_phone;