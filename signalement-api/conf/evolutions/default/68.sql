-- !Ups

ALTER TABLE reports
ADD COLUMN ccrf_code TEXT[] DEFAULT '{}'::TEXT[];

-- !Downs

ALTER TABLE reports
DROP COLUMN ccrf_code