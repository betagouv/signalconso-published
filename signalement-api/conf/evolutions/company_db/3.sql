# --- !Ups

CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

CREATE INDEX IF NOT EXISTS etab_denom_trgm_idx ON etablissements USING GIST (denominationusuelleetablissement gist_trgm_ops);
CREATE INDEX IF NOT EXISTS etab_enseigne_trgm_idx ON etablissements USING GIST (enseigne1etablissement gist_trgm_ops);
CREATE INDEX IF NOT EXISTS etab_cp_idx ON etablissements USING GIN (codepostaletablissement);
CREATE INDEX IF NOT EXISTS etab_siret_idx ON etablissements USING GIN (siret);
CREATE INDEX IF NOT EXISTS etab_siren_idx ON etablissements USING GIN (siren);


# --- !Downs