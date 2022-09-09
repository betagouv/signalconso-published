# --- !Ups

create unique index no_similar_report on signalement (email, nom, prenom, details, date_trunc('day'::text, date_creation), adresse_etablissement);

# --- !Downs

drop index no_similar_report;