-- !Ups

UPDATE events SET event_type = 'ADMIN' where event_type = 'RECTIF';
UPDATE events SET event_type = 'DGCCRF', action = 'Ajout d''un commentaire' where action = 'Ajout d''un commentaire interne à la DGCCRF';

-- !Downs

