# --- !Ups

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS etablissements  (
    id UUID DEFAULT UUID_GENERATE_V4(),
    siren VARCHAR,
    nic VARCHAR,
    siret VARCHAR,
    statutDiffusionEtablissement VARCHAR,
    dateCreationEtablissement VARCHAR,
    trancheEffectifsEtablissement VARCHAR,
    anneeEffectifsEtablissement VARCHAR,
    activitePrincipaleRegistreMetiersEtablissement VARCHAR,
    dateDernierTraitementEtablissement VARCHAR,
    etablissementSiege VARCHAR,
    nombrePeriodesEtablissement VARCHAR,
    complementAdresseEtablissement VARCHAR,
    numeroVoieEtablissement VARCHAR,
    indiceRepetitionEtablissement VARCHAR,
    typeVoieEtablissement VARCHAR,
    libelleVoieEtablissement VARCHAR,
    codePostalEtablissement VARCHAR,
    libelleCommuneEtablissement VARCHAR,
    libelleCommuneEtrangerEtablissement VARCHAR,
    distributionSpecialeEtablissement VARCHAR,
    codeCommuneEtablissement VARCHAR,
    codeCedexEtablissement VARCHAR,
    libelleCedexEtablissement VARCHAR,
    codePaysEtrangerEtablissement VARCHAR,
    libellePaysEtrangerEtablissement VARCHAR,
    complementAdresse2Etablissement VARCHAR,
    numeroVoie2Etablissement VARCHAR,
    indiceRepetition2Etablissement VARCHAR,
    typeVoie2Etablissement VARCHAR,
    libelleVoie2Etablissement VARCHAR,
    codePostal2Etablissement VARCHAR,
    libelleCommune2Etablissement VARCHAR,
    libelleCommuneEtranger2Etablissement VARCHAR,
    distributionSpeciale2Etablissement VARCHAR,
    codeCommune2Etablissement VARCHAR,
    codeCedex2Etablissement VARCHAR,
    libelleCedex2Etablissement VARCHAR,
    codePaysEtranger2Etablissement VARCHAR,
    libellePaysEtranger2Etablissement VARCHAR,
    dateDebut VARCHAR,
    etatAdministratifEtablissement VARCHAR,
    enseigne1Etablissement VARCHAR,
    enseigne2Etablissement VARCHAR,
    enseigne3Etablissement VARCHAR,
    denominationUsuelleEtablissement VARCHAR,
    activitePrincipaleEtablissement VARCHAR,
    nomenclatureActivitePrincipaleEtablissement VARCHAR,
    caractereEmployeurEtablissement VARCHAR
);


CREATE TABLE IF NOT EXISTS activites (
    code VARCHAR,
    libelle VARCHAR
)

# --- !Downs