package company

import java.net.URL

sealed trait CompanyFile {
  val url: URL
  val name: String
  val approximateSize: Double
  val headers: Seq[String]
}

object EtablissementFile extends CompanyFile {

  override val url: URL = new URL("https://files.data.gouv.fr/insee-sirene/StockEtablissement_utf8.zip")
  override val name: String = "StockEtablissement"
  override val approximateSize: Double = 32e6
  override val headers: Seq[String] = Seq(
    "siren",
    "nic",
    "siret",
    "statutdiffusionetablissement",
    "datecreationetablissement",
    "trancheeffectifsetablissement",
    "anneeeffectifsetablissement",
    "activiteprincipaleregistremetiersetablissement",
    "datederniertraitementetablissement",
    "etablissementsiege",
    "nombreperiodesetablissement",
    "complementadresseetablissement",
    "numerovoieetablissement",
    "indicerepetitionetablissement",
    "typevoieetablissement",
    "libellevoieetablissement",
    "codepostaletablissement",
    "libellecommuneetablissement",
    "libellecommuneetrangeretablissement",
    "distributionspecialeetablissement",
    "codecommuneetablissement",
    "codecedexetablissement",
    "libellecedexetablissement",
    "codepaysetrangeretablissement",
    "libellepaysetrangeretablissement",
    "complementadresse2etablissement",
    "numerovoie2etablissement",
    "indicerepetition2etablissement",
    "typevoie2etablissement",
    "libellevoie2etablissement",
    "codepostal2etablissement",
    "libellecommune2etablissement",
    "libellecommuneetranger2etablissement",
    "distributionspeciale2etablissement",
    "codecommune2etablissement",
    "codecedex2etablissement",
    "libellecedex2etablissement",
    "codepaysetranger2etablissement",
    "libellepaysetranger2etablissement",
    "datedebut",
    "etatadministratifetablissement",
    "enseigne1etablissement",
    "enseigne2etablissement",
    "enseigne3etablissement",
    "denominationusuelleetablissement",
    "activiteprincipaleetablissement",
    "nomenclatureactiviteprincipaleetablissement",
    "caractereemployeuretablissement"
  )
}

object UniteLegaleFile extends CompanyFile {
  override val url: URL = new URL("https://files.data.gouv.fr/insee-sirene/StockUniteLegale_utf8.zip")
  override val name: String = "StockUniteLegale"
  override val approximateSize: Double = 23e6
  override val headers: Seq[String] = Seq(
    "siren",
    "statutdiffusionunitelegale",
    "unitepurgeeunitelegale",
    "datecreationunitelegale",
    "sigleunitelegale",
    "sexeunitelegale",
    "prenom1unitelegale",
    "prenom2unitelegale",
    "prenom3unitelegale",
    "prenom4unitelegale",
    "prenomusuelunitelegale",
    "pseudonymeunitelegale",
    "identifiantassociationunitelegale",
    "trancheeffectifsunitelegale",
    "anneeeffectifsunitelegale",
    "datederniertraitementunitelegale",
    "nombreperiodesunitelegale",
    "categorieentreprise",
    "anneecategorieentreprise",
    "datedebut",
    "etatadministratifunitelegale",
    "nomunitelegale",
    "nomusageunitelegale",
    "denominationunitelegale",
    "denominationusuelle1unitelegale",
    "denominationusuelle2unitelegale",
    "denominationusuelle3unitelegale",
    "categoriejuridiqueunitelegale",
    "activiteprincipaleunitelegale",
    "nomenclatureactiviteprincipaleunitelegale",
    "nicsiegeunitelegale",
    "economiesocialesolidaireunitelegale",
    "caractereemployeurunitelegale"
  )
}
