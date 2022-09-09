-- !Ups

ALTER TABLE reports
ADD COLUMN reponseconso_code TEXT[] DEFAULT '{}'::TEXT[];

-- !Downs

ALTER TABLE reports
DROP COLUMN reponseconso_code