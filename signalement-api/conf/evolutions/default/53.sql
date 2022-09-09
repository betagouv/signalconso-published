-- !Ups

CREATE TABLE emails_validation
(
    id                   UUID        NOT NULL PRIMARY KEY,
    creation_date        TIMESTAMPTZ NOT NULL,
    confirmation_code    VARCHAR     NOT NULL,
    attempts             INTEGER DEFAULT 0,
    email                VARCHAR     NOT NULL UNIQUE,
    last_attempt         TIMESTAMPTZ,
    last_validation_date TIMESTAMPTZ
);

-- !Downs

DROP TABLE emails_validation


