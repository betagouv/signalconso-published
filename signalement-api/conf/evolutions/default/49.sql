-- !Ups

UPDATE websites
SET kind = 'DEFAULT'
WHERE kind = 'EXCLUSIVE';

-- !Downs
