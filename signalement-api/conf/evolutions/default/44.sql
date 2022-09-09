-- !Ups

ALTER TABLE reports ADD COLUMN tags TEXT[] DEFAULT '{}'::TEXT[];

-- !Downs

ALTER TABLE reports DROP COLUMN tags;