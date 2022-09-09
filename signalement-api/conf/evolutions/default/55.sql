-- !Ups

ALTER TABLE reports ADD COLUMN forward_to_reponseconso BOOLEAN NOT NULL DEFAULT false;

-- !Downs

ALTER TABLE reports DROP COLUMN forward_to_reponseconso;