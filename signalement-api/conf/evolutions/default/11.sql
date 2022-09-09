# --- !Ups

update signalement set categorie = 'Autre' where categorie = 'Autres';

update signalement set categorie = 'Matériel et objets' where categorie = 'Matériel / Objet';

update signalement set categorie = 'Nourriture et boissons' where categorie = 'Nourriture / Boissons';

update signalement set categorie = 'Prix et paiement' where categorie = 'Prix / Paiement';

update signalement set categorie = 'Publicité' where categorie = 'Publicité (affiche, catalogue..)';

# --- !Downs
