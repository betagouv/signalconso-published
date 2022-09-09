-- !Ups

ALTER TABLE subscriptions ADD COLUMN sirets VARCHAR[] DEFAULT '{}'::varchar[];
ALTER TABLE subscriptions ADD COLUMN frequency INTERVAL;

UPDATE subscriptions SET frequency = interval '1 days' WHERE categories <> '{}'::varchar[];
UPDATE subscriptions SET frequency = interval '7 days' WHERE categories = '{}'::varchar[];

ALTER TABLE subscriptions ALTER COLUMN frequency SET NOT NULL;

-- !Downs

ALTER TABLE subscriptions DROP COLUMN sirets;
ALTER TABLE subscriptions DROP COLUMN frequency;
