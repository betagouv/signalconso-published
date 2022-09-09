# --- !Ups

ALTER TABLE SIGNALEMENT ALTER COLUMN type_etablissement DROP NOT NULL;
ALTER TABLE SIGNALEMENT RENAME COLUMN categorie_anomalie TO categorie;
ALTER TABLE SIGNALEMENT RENAME COLUMN precision_anomalie TO sous_categorie;
ALTER TABLE SIGNALEMENT RENAME COLUMN siren_etablissement TO siret_etablissement;
ALTER TABLE SIGNALEMENT ADD COLUMN precision VARCHAR;

# --- !Downs

ALTER TABLE SIGNALEMENT RENAME COLUMN categorie TO categorie_anomalie;
ALTER TABLE SIGNALEMENT RENAME COLUMN sous_categorie TO precision_anomalie;
ALTER TABLE SIGNALEMENT RENAME COLUMN siret_etablissement TO siren_etablissement;
ALTER TABLE SIGNALEMENT DROP precision;
