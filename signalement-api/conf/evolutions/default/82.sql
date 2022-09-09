-- !Ups

ALTER TABLE companies ADD COLUMN is_headoffice BOOLEAN not null default false ;
ALTER TABLE companies ADD COLUMN is_open BOOLEAN not null default true;

-- !Downs

