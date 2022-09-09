-- !Ups

ALTER TABLE events ADD COLUMN company_id UUID;

UPDATE events e
SET company_id = subquery.company_id
FROM (SELECT id, company_id FROM reports r) AS subquery
WHERE e.report_id = subquery.id;

-- !Downs

ALTER TABLE events DROP COLUMN company_id;
