-- !Ups

update subscriptions set tags = array_replace(tags, 'Litige contractuel','LitigeContractuel');
update subscriptions set tags = array_replace(tags, 'hygiène','Hygiene');
update subscriptions set tags = array_replace(tags, 'Produit dangereux','ProduitDangereux');
update subscriptions set tags = array_replace(tags, 'Démarchage à domicile','DemarchageADomicile');
update subscriptions set tags = array_replace(tags, 'Démarchage téléphonique','DemarchageTelephonique');
update subscriptions set tags = array_replace(tags, 'Absence de médiateur','AbsenceDeMediateur');
update subscriptions set tags = array_replace(tags, 'Produit industriel','ProduitIndustriel');
update subscriptions set tags = array_replace(tags, 'Produit alimentaire','ProduitAlimentaire');
update subscriptions set tags = array_replace(tags, 'Produit Alimentaire','ProduitAlimentaire');
update subscriptions set tags = array_replace(tags, 'Produit Alimenaire','ProduitAlimentaire');
update subscriptions set tags = array_replace(tags, 'Compagnie aerienne','CompagnieAerienne');

-- !Downs

update subscriptions set tags = array_replace(tags, 'LitigeContractuel','Litige contractuel');
update subscriptions set tags = array_replace(tags, 'Hygiene','hygiène');
update subscriptions set tags = array_replace(tags, 'ProduitDangereux','Produit dangereux');
update subscriptions set tags = array_replace(tags, 'DemarchageADomicile','Démarchage à domicile');
update subscriptions set tags = array_replace(tags, 'DemarchageTelephonique','Démarchage téléphonique');
update subscriptions set tags = array_replace(tags, 'AbsenceDeMediateur','Absence de médiateur');
update subscriptions set tags = array_replace(tags, 'ProduitIndustriel','Produit industriel');
update subscriptions set tags = array_replace(tags, 'ProduitAlimentaire','Produit Alimentaire');
update subscriptions set tags = array_replace(tags, 'CompagnieAerienne','Compagnie aerienne');