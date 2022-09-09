# --- !Ups

ALTER TABLE etablissements
    ADD CONSTRAINT etablissements_pk PRIMARY KEY (siret);

CREATE TABLE IF NOT EXISTS etablissements_import_info
(
    id          UUID PRIMARY KEY,
    file_name   VARCHAR                     NOT NULL,
    file_url    VARCHAR                     NOT NULL,
    lines_count INTEGER                     NOT NULL,
    lines_done  INTEGER                     NOT NULL,
    started_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    ended_at    TIMESTAMP WITHOUT TIME ZONE,
    errors      VARCHAR
);

# --- !Downs

ALTER TABLE etablissements
    DROP CONSTRAINT etablissements_pk;

DROP TABLE etablissements_import_info