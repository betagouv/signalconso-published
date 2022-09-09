-- !Ups

ALTER TABLE companies ADD COLUMN department VARCHAR;

UPDATE companies SET department = '978' WHERE postal_code = '97150';
UPDATE companies SET department = '977' WHERE postal_code = '97133';
UPDATE companies SET department = '2A' WHERE postal_code like '200%';
UPDATE companies SET department = '2A' WHERE postal_code like '201%';
UPDATE companies SET department = '2B' WHERE postal_code like '202%';
UPDATE companies SET department = substring(postal_code, 1, 2) WHERE department is null and postal_code is not null;

-- !Downs

ALTER TABLE companies DROP COLUMN department;