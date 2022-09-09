-- !Ups
ALTER TABLE websites ADD COLUMN IF NOT EXISTS identification_status varchar not null default 'NotIdentified';
ALTER TABLE websites ADD COLUMN IF NOT EXISTS is_marketplace bool default false;
ALTER TABLE websites ALTER COLUMN kind DROP NOT NULL;
ALTER TABLE websites ALTER COLUMN kind DROP DEFAULT;

update websites set is_marketplace = true, identification_status = 'Identified'
where kind = 'MARKETPLACE'
and is_marketplace = false;

update websites set identification_status = 'Identified'
where kind = 'DEFAULT'
and identification_status = 'NotIdentified';

-- !Downs

