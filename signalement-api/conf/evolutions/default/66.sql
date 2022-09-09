-- !Ups

ALTER TABLE companies DROP IF EXISTS address_old_version;
ALTER TABLE companies DROP IF EXISTS postal_code_old_version;
ALTER TABLE companies DROP IF EXISTS department_old_version;
ALTER TABLE companies DROP IF EXISTS done;

ALTER TABLE reports DROP IF EXISTS company_address_old_version;
ALTER TABLE reports DROP IF EXISTS company_postal_code_old_version;
ALTER TABLE reports DROP IF EXISTS done;

DROP INDEX IF EXISTS no_similar_report;

-- The where clause is needed because index was broken so there is many duplicates
CREATE UNIQUE index no_similar_report ON reports (
                                                  email,
                                                  last_name,
                                                  first_name,
                                                  details,
                                                  my_date_trunc('day'::text, creation_date),
                                                  company_postal_code,
                                                  company_street_number,
                                                  company_street,
                                                  company_address_supplement,
                                                  company_city
    ) WHERE creation_date > '2021-12-15'::TIMESTAMP AT TIME ZONE 'utc';

-- !Downs


ALTER TABLE companies ADD COLUMN address_old_version VARCHAR;
ALTER TABLE companies ADD COLUMN postal_code_old_version VARCHAR;
ALTER TABLE companies ADD COLUMN department_old_version VARCHAR;
ALTER TABLE companies ADD COLUMN done VARCHAR;

ALTER TABLE reports ADD COLUMN company_address_old_version VARCHAR;
ALTER TABLE reports ADD COLUMN company_postal_code_old_version VARCHAR;
ALTER TABLE reports ADD COLUMN done VARCHAR;

DROP INDEX IF EXISTS no_similar_report;

CREATE UNIQUE index no_similar_report
    ON reports (email, last_name, first_name, details, my_date_trunc('day'::text, creation_date), company_address_old_version);
