-- !Ups

ALTER TABLE websites ADD COLUMN company_country VARCHAR;
ALTER TABLE websites ALTER COLUMN company_id DROP NOT NULL;

-- !Downs

ALTER TABLE websites DROP COLUMN company_country;