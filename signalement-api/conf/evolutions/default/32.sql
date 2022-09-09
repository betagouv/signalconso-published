-- !Ups

ALTER TABLE subscriptions ADD COLUMN email VARCHAR;
ALTER TABLE subscriptions ADD CONSTRAINT subscriptions_unique UNIQUE(email, category);

-- !Downs

ALTER TABLE subscriptions DROP COLUMN email;
