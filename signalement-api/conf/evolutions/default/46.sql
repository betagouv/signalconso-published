-- !Ups

ALTER TABLE report_files ADD COLUMN av_output VARCHAR;
UPDATE report_files SET av_output = '—' WHERE av_output IS NULL;

ALTER TABLE websites ADD COLUMN kind VARCHAR NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE websites ADD COLUMN host VARCHAR;
ALTER TABLE websites ALTER COLUMN url DROP NOT NULL;

ALTER TABLE reports ADD COLUMN vendor VARCHAR;

UPDATE reports SET tags = tags || '{Internet}'::text[] WHERE website_url IS NOT NULL AND NOT(tags && '{Internet}'::text[]);

-- !Downs

ALTER TABLE websites DROP COLUMN kind;
ALTER TABLE websites DROP COLUMN host;

ALTER TABLE reports DROP COLUMN vendor;
                                                                                             
ALTER TABLE report_files DROP COLUMN av_output;
