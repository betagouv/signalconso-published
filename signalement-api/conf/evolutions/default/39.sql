-- !Ups

ALTER TABLE subscriptions DROP COLUMN category;
ALTER TABLE subscriptions RENAME COLUMN values TO departments;
ALTER TABLE subscriptions ADD COLUMN categories VARCHAR[] DEFAULT '{}'::varchar[];

-- !Downs

ALTER TABLE subscriptions DROP COLUMN categories;
ALTER TABLE subscriptions RENAME COLUMN departments TO values;
ALTER TABLE subscriptions ADD COLUMN category VARCHAR[];

