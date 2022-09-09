-- !Ups

update reports set tags = array_replace(tags, 'Litige contractuel','LitigeContractuel');
update reports set tags = array_replace(tags, 'hygiène','Hygiene');
update reports set tags = array_replace(tags, 'Produit dangereux','ProduitDangereux');
update reports set tags = array_replace(tags, 'Démarchage à domicile','DemarchageADomicile');
update reports set tags = array_replace(tags, 'Démarchage téléphonique','DemarchageTelephonique');
update reports set tags = array_replace(tags, 'Absence de médiateur','AbsenceDeMediateur');
update reports set tags = array_replace(tags, 'Produit industriel','ProduitIndustriel');
update reports set tags = array_replace(tags, 'Produit alimentaire','ProduitAlimentaire');
update reports set tags = array_replace(tags, 'Produit Alimentaire','ProduitAlimentaire');
update reports set tags = array_replace(tags, 'Produit Alimenaire','ProduitAlimentaire');
update reports set tags = array_replace(tags, 'Compagnie aerienne','CompagnieAerienne');

-- !Downs

update reports set tags = array_replace(tags, 'LitigeContractuel','Litige contractuel');
update reports set tags = array_replace(tags, 'Hygiene','hygiène');
update reports set tags = array_replace(tags, 'ProduitDangereux','Produit dangereux');
update reports set tags = array_replace(tags, 'DemarchageADomicile','Démarchage à domicile');
update reports set tags = array_replace(tags, 'DemarchageTelephonique','Démarchage téléphonique');
update reports set tags = array_replace(tags, 'AbsenceDeMediateur','Absence de médiateur');
update reports set tags = array_replace(tags, 'ProduitIndustriel','Produit industriel');
update reports set tags = array_replace(tags, 'ProduitAlimentaire','Produit Alimentaire');
update reports set tags = array_replace(tags, 'CompagnieAerienne','Compagnie aerienne');