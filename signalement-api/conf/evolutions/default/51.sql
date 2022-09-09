-- !Ups

ALTER TABLE "reports" ADD COLUMN "phone" VARCHAR;


-- !Downs

ALTER TABLE "reports" DROP COLUMN "phone";
