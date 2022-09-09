-- !Ups

update reports set tags = array_replace(tags, 'Démarcharge à domicile','DemarchageADomicile');

-- !Downs

update reports set tags = array_replace(tags, 'DemarchageADomicile','Démarchage à domicile');