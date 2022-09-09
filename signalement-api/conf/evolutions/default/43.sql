-- !Ups

UPDATE events SET action = 'Envoi du courrier d''activation' WHERE action = 'Envoi d''un courrier';
UPDATE events SET action = 'Première consultation du signalement par le professionnel' WHERE action = 'Envoi du signalement';
UPDATE events SET action = 'Email « Accusé de réception » envoyé au consommateur' WHERE action = 'Envoi email accusé de réception';
UPDATE events SET action = 'Email « Nouveau signalement » envoyé au professionnel' WHERE action = 'Envoi d''un email';
UPDATE events SET action = 'Email « Signalement consulté » envoyé au consommateur' WHERE action = 'Envoi email d''information de transmission';
UPDATE events SET action = 'Email « L''entreprise a répondu à votre signalement » envoyé au consommateur' WHERE action = 'Envoi email de la réponse pro';
UPDATE events SET action = 'Email « Nouveau signalement non consulté » envoyé au professionnel' WHERE action = 'Relance';

-- !Downs

UPDATE events SET action = 'Envoi d''un courrier' WHERE action = 'Envoi du courrier d''activation';
UPDATE events SET action = 'Envoi du signalement' WHERE action = 'Première consultation du signalement par le professionnel';
UPDATE events SET action = 'Envoi email accusé de réception' WHERE action = 'Email « Accusé de réception » envoyé au consommateur';
UPDATE events SET action = 'Envoi d''un email' WHERE action = 'Email « Nouveau signalement » envoyé au professionnel';
UPDATE events SET action = 'Envoi email d''information de transmission' WHERE action = 'Email « Signalement consulté » envoyé au consommateur';
UPDATE events SET action = 'Envoi email de la réponse pro' WHERE action = 'Email « L''entreprise a répondu à votre signalement » envoyé au consommateur';
UPDATE events SET action = 'Relance' WHERE action = 'Email « Nouveau signalement non consulté » envoyé au professionnel';

