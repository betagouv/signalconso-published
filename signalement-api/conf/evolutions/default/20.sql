-- !Ups

CREATE TABLE "companies" (
    "id" UUID NOT NULL PRIMARY KEY,
    "siret" VARCHAR NOT NULL UNIQUE,
    "creation_date" timestamptz NOT NULL,
    "name" VARCHAR NOT NULL,
    "address" VARCHAR NOT NULL,
    "postal_code" VARCHAR
);

ALTER TABLE "signalement" ADD COLUMN "company_id" UUID REFERENCES companies(id);

CREATE TABLE "company_accesses" (
    "company_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "level" VARCHAR NOT NULL,
    "update_date" TIMESTAMPTZ NOT NULL
);
ALTER TABLE "company_accesses" ADD CONSTRAINT "pk_company_user" PRIMARY KEY("company_id","user_id");
ALTER TABLE "company_accesses" ADD CONSTRAINT "COMPANY_FK" FOREIGN KEY("company_id") REFERENCES "companies"("id") ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE "company_accesses" ADD CONSTRAINT "USER_FK" FOREIGN KEY("user_id") REFERENCES "users"("id") ON UPDATE CASCADE ON DELETE CASCADE;

CREATE TABLE "company_access_tokens" (
    "id" UUID NOT NULL PRIMARY KEY,
    "company_id" UUID NOT NULL,
    "token" VARCHAR NOT NULL,
    "level" VARCHAR NOT NULL,
    "valid" BOOLEAN NOT NULL,
    "expiration_date" TIMESTAMPTZ
);
ALTER TABLE "company_access_tokens" ADD CONSTRAINT "COMPANY_FK" FOREIGN KEY("company_id") REFERENCES "companies"("id") ON UPDATE CASCADE ON DELETE CASCADE;

-- !Downs

ALTER TABLE "signalement" DROP COLUMN "company_id";

DROP TABLE "company_accesses";
DROP TABLE "company_access_tokens";
DROP TABLE "companies";
