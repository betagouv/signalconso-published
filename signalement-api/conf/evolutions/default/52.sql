-- !Ups

ALTER TABLE subscriptions ADD COLUMN creation_date TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE subscriptions ADD COLUMN tags VARCHAR[] DEFAULT '{}'::varchar[];
ALTER TABLE subscriptions ADD COLUMN countries VARCHAR[] DEFAULT '{}'::varchar[];

-- !Downs

ALTER TABLE subscriptions DROP COLUMN creation_date;
ALTER TABLE subscriptions DROP COLUMN tags;
ALTER TABLE subscriptions DROP COLUMN countries;


