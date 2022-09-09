-- !Ups

UPDATE reports set status = 'LanceurAlerte' where status = 'Lanceur d''alerte';
UPDATE reports set status = 'TraitementEnCours' where status = 'Traitement en cours';
UPDATE reports set status = 'Transmis' where status = 'Signalement transmis';
UPDATE reports set status = 'PromesseAction' where status = 'Promesse action';
UPDATE reports set status = 'Infonde' where status = 'Signalement infondé';
UPDATE reports set status = 'NonConsulte' where status = 'Signalement non consulté';
UPDATE reports set status = 'ConsulteIgnore' where status = 'Signalement consulté ignoré';
UPDATE reports set status = 'MalAttribue' where status = 'Signalement mal attribué';

-- !Downs

UPDATE reports set status = 'Lanceur d''alerte' where status = 'LanceurAlerte';
UPDATE reports set status = 'Traitement en cours' where status = 'TraitementEnCours';
UPDATE reports set status = 'Signalement transmis' where status = 'Transmis';
UPDATE reports set status = 'Promesse action' where status = 'PromesseAction';
UPDATE reports set status = 'Signalement infondé' where status = 'Infonde';
UPDATE reports set status = 'Signalement non consulté' where status = 'NonConsulte';
UPDATE reports set status = 'Signalement consulté ignoré' where status = 'ConsulteIgnore';
UPDATE reports set status = 'Signalement mal attribué' where status = 'MalAttribue';
