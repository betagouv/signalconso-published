# --- !Ups

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS siret_idx ON etablissements (siret);

# --- !Downs

DROP INDEX CONCURRENTLY IF EXISTS siret_idx;
