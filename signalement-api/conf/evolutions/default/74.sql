-- !Ups

ALTER TABLE subscriptions DROP COLUMN tags;
ALTER TABLE subscriptions ADD COLUMN with_tags VARCHAR[] DEFAULT '{}'::varchar[];
ALTER TABLE subscriptions ADD COLUMN without_tags VARCHAR[] DEFAULT '{}'::varchar[];

-- !Downs

ALTER TABLE subscriptions DROP COLUMN with_tags;
ALTER TABLE subscriptions DROP COLUMN without_tags;
ALTER TABLE subscriptions ADD COLUMN tags VARCHAR[] DEFAULT '{}'::varchar[];