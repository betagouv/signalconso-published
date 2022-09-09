# --- !Ups

ALTER TABLE SIGNALEMENT RENAME COLUMN status_pro TO status;

# --- !Downs

ALTER TABLE SIGNALEMENT RENAME COLUMN status TO status_pro;
