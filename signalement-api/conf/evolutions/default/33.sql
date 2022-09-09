-- !Ups

CREATE TABLE "async_files" (
    "id" UUID NOT NULL PRIMARY KEY,
    "user_id" UUID NOT NULL,
    "creation_date" timestamptz NOT NULL,
    "filename" VARCHAR,
    "storage_filename" VARCHAR
);

-- !Downs

DROP TABLE async_files;
