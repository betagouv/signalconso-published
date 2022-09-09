-- !Ups

ALTER TABLE report_data
DROP CONSTRAINT report_data_report_id_fkey,
ADD CONSTRAINT report_data_report_id_fkey
   FOREIGN KEY (report_id)
   REFERENCES reports(id)
   ON DELETE CASCADE;

-- !Downs

