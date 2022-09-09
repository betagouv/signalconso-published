-- !Ups

ALTER TABLE websites DROP COLUMN url;
ALTER TABLE reports DROP COLUMN website_id;

DELETE FROM websites WHERE company_id is null;

ALTER TABLE websites ALTER COLUMN company_id SET NOT NULL;

-- !Downs

ALTER TABLE reports ADD COLUMN website_id UUID REFERENCES websites(id);
ALTER TABLE websites ADD COLUMN url VARCHAR;
