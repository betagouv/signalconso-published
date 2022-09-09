-- !Ups

ALTER TABLE reports
    ADD COLUMN company_activity_code VARCHAR;

update reports
set company_activity_code = c.activity_code
from companies c where reports.company_id = c.id;

-- !Downs

ALTER TABLE reports DROP COLUMN company_activity_code;