-- !Ups

ALTER TABLE async_files ADD COLUMN kind VARCHAR;
UPDATE async_files SET kind = 'ReportedPhones' WHERE filename LIKE 'telephones-signales-%';
UPDATE async_files SET kind = 'Reports' WHERE filename LIKE 'signalements-%';
UPDATE async_files SET kind = 'ReportedWebsites' WHERE filename LIKE 'sites-non-identifies-%';
UPDATE async_files SET kind = 'Reports' WHERE filename IS NULl;

-- !Downs

ALTER TABLE async_files DROP COLUMN kind;
