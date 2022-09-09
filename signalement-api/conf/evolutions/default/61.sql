-- !Ups

DROP TABLE report_data

-- !Downs

CREATE TABLE report_data (
    report_id UUID NOT NULL PRIMARY KEY,
    read_delay BIGINT,
    response_delay BIGINT
);

ALTER TABLE report_data
ADD CONSTRAINT report_data_report_id_fkey
   FOREIGN KEY (report_id)
   REFERENCES reports(id)
   ON DELETE CASCADE;
