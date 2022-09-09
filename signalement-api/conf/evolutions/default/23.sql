-- !Ups

ALTER TABLE company_access_tokens ADD COLUMN emailed_to VARCHAR;

-- !Downs

ALTER TABLE company_access_tokens DROP COLUMN emailed_to;
