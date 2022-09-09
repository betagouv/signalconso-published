# SignalConso API

API de l'outil SignalConso (ex signalement).

L’outil SignalConso permet à chaque consommateur de signaler directement les anomalies constatées dans sa vie de tous les jours (chez son épicier, dans un bar..), de manière très rapide et très simple auprès du professionnel.

Plus d'information ici : https://beta.gouv.fr/startup/signalement.html

L'API nécessite une base PostgreSQL pour la persistence des données (versions supportées : 9.5+).

Le build se fait à l'aide de [SBT](https://www.scala-sbt.org/) (voir [build.sbt])

## Développement

### PostgreSQL

L'application requiert une connexion à un serveur PostgreSQL (sur macOS, vous pouvez utiliser [https://postgresapp.com/]).
Créez une base de données pour l'application : `createdb signalconso` (par défaut localement, la base sera accessible au user `$USER`, sans mot de passe).


Il est possible de lancer un PostgreSQL à partir d'une commande docker-compose (le fichier en question est disponible sous scripts/local/)

à la racine du projet faire :
```
docker-compose -f scripts/local/docker-compose.yml up

```
Au lancement du programme, les tables seront automatiquement créées si elles n'existent pas (voir [https://www.playframework.com/documentation/2.7.x/Evolutions]  **et s'assurer que les properties play.evolutions sont a true**).

Il est possible d'injecter des données de test dans la base signal conso, pour cela il faut jouer les scripts suivants :

- /test/scripts/insert_users.sql
- /test/scripts/insert_companies.sql
- /test/scripts/insert_company_accesses.sql
- /test/scripts/insert_reports.sql


### Configuration locale

Lancer une base de donnes PosgreSQL provisionée avec les tables et données (voir plus haut)

L'application a besoin de variables d'environnements. Vous devez les configurer. Il y a plusieurs manières de faire, nous recommandons la suivante :

```bash
# à ajouter dans votre .zprofile, .zshenv .bash_profile, ou équivalent
# pour toutes les valeurs avec XXX, vous devez renseigner des vraies valeurs.
# Vous pouvez par exemple reprendre les valeurs de l'environnement de démo dans Clever Cloud

function scsbt {
  # Set all environnements variables for the api then launch sbt
  # It forwards arguments, so you can do "scsbt", "scscbt compile", etc.
  echo "Launching sbt with extra environnement variables"
  MAILER_HOST="XXX" \
  MAILER_PORT="XXX" \
  MAILER_SSL="yes" \
  MAILER_TLS="no" \
  MAILER_TLS_REQUIRED="no" \
  MAILER_USER="XXX" \
  MAILER_PASSWORD="XXX" \
  MAILER_MOCK="yes" \
  OUTBOUND_EMAIL_FILTER_REGEX="beta?.gouv|@.*gouv.fr" \
  SIGNAL_CONSO_SCHEDULED_JOB_ACTIVE="false" \
  MAIL_FROM="XXX" \
  MAIL_CONTACT_ADDRESS="XXX" \
  EVOLUTIONS_ENABLED=true \
  EVOLUTIONS_AUTO_APPLY=false \
  EVOLUTIONS_AUTO_APPLY_DOWNS=false \
  TMP_DIR="/tmp/" \
  S3_ACCESS_KEY_ID="XXX" \
  S3_SECRET_ACCESS_KEY="XXX" \
  COMPANY_DATABASE_URL="XXX" \
  DATABASE_URL="XXX" \
  sbt "$@"
}

```
Ceci définit une commande `scsbt`, à utiliser à la place de `sbt`

#### ❓ Pourquoi définir cette fonction, pourquoi ne pas juste exporter les variables en permanence ?

Pour éviter que ces variables ne soient lisibles dans l'environnement par n'importe quel process lancés sur votre machine. Bien sûr c'est approximatif, on ne peut pas empêcher un process de parser le fichier de conf directement, mais c'est déjà un petit niveau de protection.

#### ❓ Puis-je mettre ces variables dans un fichier local dans le projet, que j'ajouterai au .gitignore ?

C'est trop dangereux. Nos repos sont publics, la moindre erreur humaine au niveau du .gitignore pourrait diffuser toutes les variables.

### Lancer l'appli

Lancer

```bash
scsbt run 
```

L'API est accessible à l'adresse `http://localhost:9000/api` avec rechargement à chaud des modifications.

## Tests

Pour exécuter les tests :

```bash
scsbt test
```

Pour éxecuter uniquement un test (donné par son nom de classe):

```bash
scsbt "testOnly *SomeTestSpec"
```

## Démo

La version de démo de l'API est accessible à l'adresse http://demo-signalement-api.beta.gouv.fr/api.

## Production

L'API de production de l'application  est accessible à l'adresse https://signal-api.conso.gouv.fr/api.

## Variables d'environnement

|Nom|Description|Valeur par défaut|
|:---|:---|:---|
|APPLICATION_HOST|Hôte du serveur hébergeant l'application||
|APPLICATION_SECRET|Clé secrète de l'application||
|EVOLUTIONS_AUTO_APPLY|Exécution automatique des scripts `upgrade` de la base de données|false|
|EVOLUTIONS_AUTO_APPLY_DOWNS|Exécution automatique des scripts `downgrade` de la base de données|false|
|MAX_CONNECTIONS|Nombre maximum de connexions ouvertes vers la base de données||
|MAIL_FROM|Expéditeur des mails|dev-noreply@signal.conso.gouv.fr|
|MAIL_CONTACT_RECIPIENT|Boite mail destinataire des mails génériques|support@signal.conso.gouv.fr|
|MAILER_HOST|Hôte du serveur de mails||
|MAILER_PORT|Port du serveur de mails||
|MAILER_USER|Nom d'utilisateur du serveur de mails||
|MAILER_PASSWORD|Mot de passe du serveur de mails||
|SENTRY_DSN|Identifiant pour intégration avec [Sentry](https://sentry.io)||
|TMP_DIR|Répertoire temporaire pour création des fichiers xlsx||
---

## Liste des API

Le retour de tous les WS (web services) est au format JSON.
Sauf mention contraire, les appels se font en GET.

Pour la plupart des WS, une authentification est nécessaire.
Il faut donc que l'appel contienne le header HTTP `X-Auth-Token`, qui doit contenir un token délivré lors de l'appel au WS authenticate (cf. infra).

### 1. API d'authentification

http://localhost:9000/api/authenticate (POST)

Headers :

- Content-Type:application/json

Exemple body de la request (JSON):

```json
{
    "email":"prenom.nom@ovh.fr",
    "password":"mon-mot-de-passe"
}
```

### 2. API Signalement

*Récupération de tous les signalements*

http://localhost:9000/api/reports

Les signalements sont rendus par page. Le retour JSON est de la forme :

```json
{
    "totalCount": 2,
    "hasNextPage": false,
    "entities": [ ... ]
}
```

- totalCount rend le nombre de résultats trouvés au total pour la requête GET envoyé à l'API, en dehors du système de pagination
- hasNextPage indique s'il existe une page suivante de résultat. L'appelant doit calculer le nouvel offset pour avoir la page suivante
- entities contient les données de signalement de la page courrante
- 250 signalements par défaut sont renvoyés


*Exemple : Récupération des 10 signalements à partir du 30ème*

```
http://localhost:9000/api/reports?offset=0&limit=10
```

- offset est ignoré s'il est négatif ou s'il dépasse le nombre de signalement
- limit est ignoré s'il est négatif. Sa valeur maximum est 250

*Exemple : Récupération des 10 signalements à partir du 30ème pour le département 49*

```
http://localhost:9000/api/reports?offset=0&limit=10&departments=49
```

Le champ departments peut contenir une liste de département séparé par `,`.

*Exemple : récupèration de tous les signalements du département 49 et 94*

```
http://localhost:9000/api/reports?offset=0&limit=10&departments=49,94
```

*Exemple : Récupération par email*

```
http://localhost:9000/api/reports?offset=0&limit=10&email=john@gmail.com
```

*Exemple : Récupération par siret*

```
http://localhost:9000/api/reports?offset=0&limit=10&siret=40305211101436
```

*Exemple : Récupération de toutes les entreprises commençant par Géant*

```
http://localhost:9000/api/reports?offset=0&limit=10&companyName=Géant
```

*Exemple : Récupération par catégorie*

```
http://localhost:9000/api/reports?offset=0&limit=10&category=Nourriture / Boissons
```

*Exemple : Récupération par statusPro*

```
http://localhost:9000/api/reports?offset=0&limit=10&statusPro=À traiter
```

*Exemple : Récupération par détails (recherche plein texte sur les colonnes sous-categories et details)*

```
http://localhost:9000/api/reports?offset=0&limit=10&details=Huwavei
```

*Suppression d'un signalement*

http://localhost:9000/api/reports/:uuid (DELETE)

Statuts :
- 204 No Content : dans le cas où la suppression fonctionne
- 404 Not Found : dans le cas où le signalement n'existe pas
- 412 Precondition Failed : statut renvoyé si des fichiers sont liés à ce signalement. Si l'on souhaite malgré cela supprimer le signalement, il faudra préalablement supprimer ces fichiers

*Modification d'un signalement*

http://localhost:9000/api/reports (PUT)

Le body envoyé doit correspondre à un signalement (de la forme renvoyée par le WS getReport.

Seul les champs suivants sont modifiables :
- firstName
- lastName
- email
- contactAgreement
- companyName
- companyAddress
- companyPostalCode
- companySiret
- statusPro

Statuts :
- 204 No Content : dans le cas où la modification fonctionne
- 404 Not Found : dans le cas où le signalement n'existe pas


### 3. API Files

*Suppression d'un fichier*

http://localhost:9000/api/reports/files/:uuid/:filename (DELETE)

```
Ex: http://localhost:9000/api/reports/files/38702d6a-907f-4ade-8e93-c4b00e668e8a/logo.png
```

Les champs `uuid` et `filename` sont obligatoires.

Statuts :
- 204 No Content : dans le cas où la suppression fonctionne
- 404 Not Found : dans le cas où le fichier n'existe pas

### 4. API Events

*Récupère la liste des évènements d'un signalement*

http://localhost:9000/api/reports/:uuidReport/events

- uuidReport: identifiant du signalement
- eventType: (optionnel) Type de l'évènement parmi : PRO, CONSO, DGCCRF

*Création d'un évènement (i. e. une action à une date)*

http://localhost:9000/api/reports/:uuidReport/events (POST)

Exemple body de la request (JSON):

```json
 {
    "userId": "e6de6b48-1c53-4d3e-a7ff-dd9b643073cf",
    "creationDate": "2019-04-14T00:00:00",
    "eventType": "PRO",
    "action": "Envoi du signalement"
}
```
- action: ce champ doit contenir le libellé d'une action pro disponible via le ws actionPros (cf. infra)

### 5. API Constantes

*La liste des actions professionnels possibles*

http://localhost:9000/api/constants/actionPros

*La liste des actions consommateurs possibles*

http://localhost:9000/api/constants/actionConsos

*La liste des statuts professionnels possibles*

http://localhost:9000/api/constants/statusPros

*La liste des statuts consommateurs possibles*

http://localhost:9000/api/constants/statusConsos
