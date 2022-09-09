-- !Ups

CREATE TABLE "auth_attempts" (
    "id" UUID NOT NULL PRIMARY KEY,
    "login" VARCHAR NOT NULL,
    "timestamp" timestamptz NOT NULL
);

-- !Downs

DROP TABLE auth_attempts;
