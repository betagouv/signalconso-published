-- !Ups

ALTER TABLE SIGNALEMENT RENAME TO REPORTS;

ALTER TABLE REPORTS RENAME COLUMN categorie TO category;
ALTER TABLE REPORTS RENAME COLUMN sous_categories TO subcategories;
ALTER TABLE REPORTS RENAME COLUMN nom_etablissement TO company_name;
ALTER TABLE REPORTS RENAME COLUMN adresse_etablissement TO company_address;
ALTER TABLE REPORTS RENAME COLUMN code_postal TO company_postal_code;
ALTER TABLE REPORTS RENAME COLUMN siret_etablissement TO company_siret;
ALTER TABLE REPORTS RENAME COLUMN date_creation TO creation_date;
ALTER TABLE REPORTS RENAME COLUMN prenom TO first_name;
ALTER TABLE REPORTS RENAME COLUMN nom TO last_name;
ALTER TABLE REPORTS RENAME COLUMN accord_contact TO contact_agreement;

ALTER TABLE REPORTS ADD COLUMN employee_consumer BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE REPORTS ALTER COLUMN employee_consumer DROP DEFAULT;

-- !Downs

ALTER TABLE REPORTS RENAME COLUMN category TO categorie;
ALTER TABLE REPORTS RENAME COLUMN subcategories TO sous_categories;
ALTER TABLE REPORTS RENAME COLUMN company_name TO nom_etablissement;
ALTER TABLE REPORTS RENAME COLUMN company_address TO adresse_etablissement;
ALTER TABLE REPORTS RENAME COLUMN company_postal_code TO code_postal;
ALTER TABLE REPORTS RENAME COLUMN company_siret TO siret_etablissement;
ALTER TABLE REPORTS RENAME COLUMN creation_date TO date_creation;
ALTER TABLE REPORTS RENAME COLUMN first_name TO prenom;
ALTER TABLE REPORTS RENAME COLUMN last_name TO nom;
ALTER TABLE REPORTS RENAME COLUMN contact_agreement TO accord_contact;

ALTER TABLE REPORTS DROP COLUMN employee_consumer;

ALTER TABLE REPORTS RENAME TO SIGNALEMENT;