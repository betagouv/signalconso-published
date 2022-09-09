import {Page} from 'shared/Layout'
import {Panel, PanelBody} from '../../shared/Panel'
import {Box, Divider, Typography} from '@mui/material'
import {styleUtils} from '../../core/theme'
import {makeSx} from '../../alexlibs/mui-extension'

const sx = makeSx({
  mark: {
    display: 'block',
    mt: 1.5,
    mb: 0.25,
    background: 'none',
    fontSize: t => styleUtils(t).fontSize.big,
  },
})

export const ModeEmploiDGCCRF = () => {
  return (
    <Page size="s">
      <Panel>
        <PanelBody>
          <Typography variant="h4" gutterBottom>
            1. Base de données
          </Typography>

          <Typography variant="h5" color="primary">
            Comment est affichée la base de données ?
          </Typography>
          <div>
            <p className="mt-4">
              Les signalements sont affichés sous forme de liste, par date de création, du plus récent au plus vieux.
              <br />
              En passant la souris sur un signalement, on peut lire le contenu plus en détails. En cliquant sur une ligne, on
              ouvre la page détaillée relative au signalement.
            </p>
          </div>

          <Divider sx={{my: 2}} />

          <Typography variant="h5" color="primary">
            Que signifie la colonne "Statut" ?
          </Typography>
          <div>
            <p>Il correspond au traitement du signalement à un instant T.</p>

            <ul>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Traitement en cours:
                </Box>
                Il s'agit d'un statut intermédiaire pendant lequel le signalement suit son cours et n'est pas clos. C'est
                notamment la période laissée au professionnel pour prendre connaissance du signalement ou y répondre.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  N.A (non-applicable):
                </Box>
                Soit les signalements sont rattachés à une URL et donc ne rentrent pas dans le flow classique d'envoi à
                l'entreprise. Soit l'entreprise signalée ne faisait pas partie du périmètre géographique de l’expérimentation au
                moment du signalement. Si la région n'a pas fait partie tout de suite de l'expérimentation, les signalements reçus
                sur cette période n'ont pas été traités et sont classés en N.A.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Promesse d'action:
                </Box>
                Le professionnel souhaite mettre en place une action préventive ou corrective.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Signalement infondé:
                </Box>
                Le professionnel a déclaré le signalement comme infondé selon lui.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Signalement non consulté:
                </Box>
                Le professionnel n'a pas créé de compte pour lire le signalement.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Signalement consulté ignoré:
                </Box>
                Le professionnel a lu le signalement mais il n'a pas répondu malgré les relances.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Signalement mal attribué:
                </Box>
                Le consommateur n'a pas sélectionné le bon établissement.
              </li>
            </ul>
          </div>

          <Divider sx={{my: 2}} />

          <Typography variant="h5" color="primary">
            Comment fonctionne le système de relance ?
          </Typography>
          <div>
            <p>Un système de relance automatique est mis en place.</p>

            <ul>
              <li>
                <strong>Le professionnel n'a pas encore de compte SignalConso : </strong>
                Un premier courrier papier a été envoyé dès réception du signalement. Après 21 jours, nous adressons un nouveau
                courrier l'incitant à créer son compte. Une nouvelle période de 21 jours est lancée, au terme de laquelle nous
                clôturons le signalement.
              </li>
              <li>
                <strong>Le professionnel a déjà ouvert un compte SignalConso : </strong>
                Qu’il s’agisse d’un nouveau signalement pas encore consulté ou d’un signalement consulté mais resté sans réponse,
                nous envoyons un email tous les 7 jours pendant 3 semaines pour inviter le professionnel à faire le nécessaire. Au
                terme de ces relances, le signalement est clôturé.
              </li>
            </ul>
          </div>

          <Divider sx={{my: 2}} />

          <Typography variant="h5" color="primary">
            Comment apporter des éléments à un signalement (courrier reçu, contrôle réalisé) ?
          </Typography>
          <div>
            <p> Il suffit de cliquer sur le bouton "Ajouter des informations DGCCRF".</p>

            <ul>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Ajout d'un commentaire interne à la DGCCRF:
                </Box>
                ce commentaire n'est visible que par la DGCCRF.
              </li>
              <li>
                <Box component="mark" sx={sx.mark}>
                  Contrôle effectué:
                </Box>
                en l'absence de rattachement avec SORA, le suivi des contrôles peut être enregistré directement sur le site.
                <br /> Cette information n'est visible que par la DGCCRF actuellement.
              </li>
            </ul>
          </div>

          <Typography variant="h4" gutterBottom>
            2. Filtres
          </Typography>

          <p>Il existe différents filtres :</p>

          <ul>
            <li>Le Département ou la Région</li>
            <li>La Catégorie ( = correspond à une icône sur la page d’accueil de SignalConso)</li>
            <li>La Période</li>
            <li>En utilisant un SIRET</li>
            <li>Par « Statut »</li>
            <li>En recherchant par mot-clé</li>
            <li>Avec l'email d'un consommateur</li>
            <li>
              En fonction du rattachement ou non à une entreprise. Si non, le signalement est actuellement rattaché seulement à
              une URL.
            </li>
          </ul>

          <p>
            La recherche par mot-clé permet de rechercher une sous-catégorie par exemple.
            <br />
            La recherche par mot-clé va rechercher dans l’ensemble du champ «&nbsp;description&nbsp;» et dans le champ
            «&nbsp;problème&nbsp;» (excepté le nom de la catégorie).
            <br />
          </p>

          <p className="notification warning">
            La recherche est insensible à la casse (majuscule/minuscule) par contre elle distingue les accents (pour le moment).
          </p>

          <Typography variant="h4" gutterBottom>
            3. Export Excel
          </Typography>

          <p>
            L’export génère un fichier Excel qui reprend les signalements suivant les filtres appliqués le cas échéant. Le fichier
            Excel comporte les informations suivantes :
          </p>

          <ul>
            <li>Date de création</li>
            <li>Département, Code postal</li>
            <li>SIRET, Nom de l'établissement, Adresse de l'établissement</li>
            <li>Catégorie, Sous-catégories</li>
            <li>Détails du signalement, Pièces jointes</li>
            <li>Statut</li>
            <li>Détail de la réponse de l'établissement (au consommateur et à la DGCCRF)</li>
            <li>Prénom, Nom, Email (du consommateur)</li>
            <li>
              Accord pour contact ( oui = le consommateur a souhaité laissé visible ses coordonnées auprès du professionnel, non =
              le consommateur a souhaité rester anonyme vis-à-vis du professionnel )
            </li>
            <li>Actions de la DGCCRF (commentaire, contrôle)</li>
          </ul>

          <Typography variant="h4" gutterBottom>
            4. Autres outils disponibles
          </Typography>

          <p>Il existe deux outils disponibles pour le moment :</p>

          <ul>
            <li>La liste des entreprises les plus signalées ;</li>
            <li>
              Un système d'abonnement. Il permet actuellement de suivre les signalements relatifs à des départements, des numéros
              SIRET ou des catégories.
              <br />
              La fréquence est soit hebdomadaire (le lundi) soit quotidienne.
            </li>
          </ul>
        </PanelBody>
      </Panel>
    </Page>
  )
}
