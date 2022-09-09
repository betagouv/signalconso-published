-- !Ups

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO companies
    (id, siret, creation_date, name, address, postal_code)
SELECT DISTINCT ON (siret_etablissement)
    UUID_GENERATE_V4(), siret_etablissement, date_creation,
    nom_etablissement, adresse_etablissement, code_postal
FROM signalement
WHERE siret_etablissement <> ''
ORDER BY siret_etablissement, date_creation DESC;

UPDATE signalement
SET company_id = companies.id
FROM companies
WHERE signalement.siret_etablissement = companies.siret;

INSERT INTO company_accesses
        (company_id, user_id, level, update_date)
SELECT
        c.id, u.id, 'admin', NOW()
FROM    users u
JOIN    companies c
ON      u.login = c.siret
WHERE   u.role = 'Professionnel';

INSERT INTO company_access_tokens
        (id, company_id, token, level, valid, expiration_date)
SELECT
        UUID_GENERATE_V4(), c.id, u.activation_key, 'admin', TRUE, c.creation_date + INTERVAL '60 DAY'
FROM    users u
JOIN    companies c
ON      u.login = c.siret
WHERE   u.role = 'ToActivate'
AND     u.activation_key <> '';

-- !Downs

TRUNCATE company_access_tokens CASCADE;
TRUNCATE company_accesses CASCADE;
UPDATE signalement SET company_id = NULL;
TRUNCATE companies CASCADE;
