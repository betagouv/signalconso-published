-- !Ups

ALTER TABLE company_access_tokens ADD COLUMN kind VARCHAR;
UPDATE company_access_tokens SET kind = CASE WHEN emailed_to IS NULL THEN 'COMPANY_INIT' ELSE 'COMPANY_JOIN' END WHERE kind IS NULL;
ALTER TABLE company_access_tokens ALTER COLUMN kind SET NOT NULL;
ALTER TABLE company_access_tokens ALTER COLUMN company_id DROP NOT NULL;
ALTER TABLE company_access_tokens ALTER COLUMN level DROP NOT NULL;
ALTER TABLE company_access_tokens RENAME TO access_tokens;

-- !Downs

ALTER TABLE access_tokens RENAME TO company_access_tokens;
ALTER TABLE company_access_tokens DROP COLUMN kind;
