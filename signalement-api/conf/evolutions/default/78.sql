-- !Ups

ALTER TABLE websites ADD COLUMN IF NOT EXISTS last_updated timestamp with time zone not null DEFAULT now();
ALTER TABLE websites ADD COLUMN IF NOT EXISTS investigation_status varchar not null default 'NotProcessed';
ALTER TABLE websites ADD COLUMN IF NOT EXISTS practice varchar;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS attribution varchar;
-- !Downs

