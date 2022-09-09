-- !Ups

ALTER TABLE reports ALTER COLUMN id SET NOT NULL;
ALTER TABLE reports ALTER COLUMN category SET NOT NULL;
ALTER TABLE reports ALTER COLUMN subcategories SET NOT NULL;
ALTER TABLE reports DROP COLUMN anomalie_file_id;
ALTER TABLE reports DROP COLUMN ticket_file_id;
ALTER TABLE reports ALTER COLUMN details SET NOT NULL;
ALTER TABLE reports DROP COLUMN piece_jointe_ids;
ALTER TABLE reports ALTER COLUMN status SET NOT NULL;
ALTER TABLE reports DROP COLUMN status_conso;

-- !Downs

ALTER TABLE reports ALTER COLUMN category DROP NOT NULL;
ALTER TABLE reports ALTER COLUMN subcategories DROP NOT NULL;
ALTER TABLE reports ADD COLUMN anomalie_file_id OID;
ALTER TABLE reports ADD COLUMN ticket_file_id OID;
ALTER TABLE reports ALTER COLUMN details DROP NOT NULL;
ALTER TABLE reports ADD COLUMN piece_jointe_ids UUID[];;
ALTER TABLE reports ALTER COLUMN status DROP NOT NULL;
ALTER TABLE reports ADD COLUMN status_conso VARCHAR;
