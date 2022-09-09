# --- !Ups

update signalement set status_pro = 'Signalement infondé' where status_pro = 'Pas de promesse d''action'

# --- !Downs

update signalement set status_pro = 'Pas de promesse d''action' where status_pro = 'Signalement infondé'