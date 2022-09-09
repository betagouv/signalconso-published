-- !Ups

ALTER TABLE reports ADD COLUMN host VARCHAR;

UPDATE reports t
SET    host = (
    SELECT (regexp_matches(website_url,'^((http[s]?|ftp):\/)?\/?(www.)?([^:\/\s]+).*$'))[4]
) where website_url is not null;

-- !Downs

ALTER TABLE reports DROP COLUMN host;
