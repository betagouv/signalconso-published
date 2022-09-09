# --- !Ups

ALTER TABLE events ADD COLUMN details JSONB;

UPDATE events SET details = json_build_object('responseType', 'ACCEPTED', 'consumerDetails', detail, 'dgccrfDetails', '')
WHERE events.action = 'Réponse du professionnel au signalement' and result_action = 'true';

UPDATE events SET details = json_build_object('responseType', 'REJECTED', 'consumerDetails', detail, 'dgccrfDetails', '')
WHERE events.action = 'Réponse du professionnel au signalement' and result_action = 'false';

UPDATE events SET details = json_build_object('description', detail)
WHERE events.action != 'Réponse du professionnel au signalement';

ALTER TABLE events DROP COLUMN detail;

# --- !Downs

ALTER TABLE events ADD COLUMN detail VARCHAR;

UPDATE events SET detail = details::jsonb->>'consumerDetails'
WHERE events.action = 'Réponse du professionnel au signalement' and result_action = 'true';

UPDATE events SET detail = details::jsonb->>'consumerDetails'
WHERE events.action = 'Réponse du professionnel au signalement' and result_action = 'false';

UPDATE events SET detail = details::jsonb->>'description'
WHERE events.action != 'Réponse du professionnel au signalement';

ALTER TABLE events DROP COLUMN details;

