-- !Ups

CREATE TABLE "websites" (
    "id" UUID NOT NULL PRIMARY KEY,
    "creation_date" timestamptz NOT NULL,
    "url" VARCHAR NOT NULL UNIQUE,
    "company_id" UUID REFERENCES companies(id)
);

ALTER TABLE "reports" ADD COLUMN "website_id" UUID REFERENCES websites(id);
ALTER TABLE "reports" ADD COLUMN "website_url" VARCHAR;
ALTER TABLE "reports" ALTER COLUMN "company_name" DROP NOT NULL;
ALTER TABLE "reports" ALTER COLUMN "company_address" DROP NOT NULL;


-- !Downs

ALTER TABLE "reports" DROP COLUMN "website_id";
ALTER TABLE "reports" DROP COLUMN "website_url";

DROP TABLE "websites";
