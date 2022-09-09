-- !Ups

ALTER TABLE company_accesses ADD COLUMN creation_date TIMESTAMPTZ NOT NULL DEFAULT now() ;

-- !Downs

ALTER TABLE company_accesses DROP COLUMN creation_date;
