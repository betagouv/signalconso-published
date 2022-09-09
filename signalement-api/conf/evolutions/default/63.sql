-- !Ups

UPDATE access_tokens
SET expiration_date = expiration_date + interval '30 day'
WHERE kind = 'COMPANY_INIT'
AND id = 'ae4142af-c55e-4b3e-897a-0e1782d83aa1';

-- !Downs

UPDATE access_tokens
SET expiration_date = expiration_date - interval '30 day'
WHERE kind = 'COMPANY_INIT'
AND id = 'ae4142af-c55e-4b3e-897a-0e1782d83aa1';
