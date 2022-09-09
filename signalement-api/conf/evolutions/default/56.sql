-- !Ups

ALTER TABLE companies RENAME COLUMN address TO address_old_version;
ALTER TABLE companies RENAME COLUMN postal_code TO postal_code_old_version;
ALTER TABLE companies RENAME COLUMN department TO department_old_version;
ALTER TABLE companies ADD COLUMN department VARCHAR;
ALTER TABLE companies ADD COLUMN street_number VARCHAR;
ALTER TABLE companies ADD COLUMN street VARCHAR;
ALTER TABLE companies ADD COLUMN address_supplement VARCHAR;
ALTER TABLE companies ADD COLUMN city VARCHAR;
ALTER TABLE companies ADD COLUMN postal_code VARCHAR;

ALTER TABLE reports RENAME COLUMN company_address TO company_address_old_version;
ALTER TABLE reports RENAME COLUMN company_postal_code TO company_postal_code_old_version;
ALTER TABLE reports ADD COLUMN company_postal_code VARCHAR;
ALTER TABLE reports ADD COLUMN company_street_number VARCHAR;
ALTER TABLE reports ADD COLUMN company_street VARCHAR;
ALTER TABLE reports ADD COLUMN company_address_supplement VARCHAR;
ALTER TABLE reports ADD COLUMN company_city VARCHAR;

ALTER TABLE companies ADD COLUMN done BOOLEAN;
ALTER TABLE reports ADD COLUMN done BOOLEAN;

-- !Downs

ALTER TABLE companies DROP COLUMN department;
ALTER TABLE companies DROP COLUMN street_number;
ALTER TABLE companies DROP COLUMN street;
ALTER TABLE companies DROP COLUMN address_supplement;
ALTER TABLE companies DROP COLUMN city;
ALTER TABLE companies DROP COLUMN postal_code;
ALTER TABLE companies RENAME COLUMN address_old_version TO address;
ALTER TABLE companies RENAME COLUMN postal_code_old_version TO postal_code;
ALTER TABLE companies RENAME COLUMN department_old_version TO department;
ALTER TABLE companies ALTER COLUMN address DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN postal_code DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN department DROP NOT NULL;

ALTER TABLE reports DROP COLUMN company_postal_code;
ALTER TABLE reports DROP COLUMN company_street_number;
ALTER TABLE reports DROP COLUMN company_street;
ALTER TABLE reports DROP COLUMN company_address_supplement;
ALTER TABLE reports DROP COLUMN company_city;
ALTER TABLE reports RENAME COLUMN company_address_old_version TO company_address;
ALTER TABLE reports RENAME COLUMN company_postal_code_old_version TO company_postal_code;
ALTER TABLE reports ALTER COLUMN company_address DROP NOT NULL;
ALTER TABLE reports ALTER COLUMN company_postal_code DROP NOT NULL;

ALTER TABLE reports DROP COLUMN done;
ALTER TABLE companies DROP COLUMN done;

