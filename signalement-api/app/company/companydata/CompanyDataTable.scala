package company.companydata

import repositories.PostgresProfile.api._
import CompanyDataTable.DENOMINATION_USUELLE_ETABLISSEMENT
import company.CompanyData
import repositories.DatabaseTable
import utils.SIREN
import utils.SIRET

class CompanyDataTable(tag: Tag) extends DatabaseTable[CompanyData](tag, "etablissements") {
  def siret = column[SIRET]("siret", O.PrimaryKey) // Primary key MUST be there so insertOrUpdateAll will do his job
  def siren = column[SIREN]("siren")
  def dateDernierTraitementEtablissement = column[Option[String]]("datederniertraitementetablissement")
  def etablissementSiege = column[Option[String]]("etablissementsiege")
  def complementAdresseEtablissement = column[Option[String]]("complementadresseetablissement")
  def numeroVoieEtablissement = column[Option[String]]("numerovoieetablissement")
  def indiceRepetitionEtablissement = column[Option[String]]("indicerepetitionetablissement")
  def typeVoieEtablissement = column[Option[String]]("typevoieetablissement")
  def libelleVoieEtablissement = column[Option[String]]("libellevoieetablissement")
  def codePostalEtablissement = column[Option[String]]("codepostaletablissement")
  def libelleCommuneEtablissement = column[Option[String]]("libellecommuneetablissement")
  def libelleCommuneEtrangerEtablissement = column[Option[String]]("libellecommuneetrangeretablissement")
  def distributionSpecialeEtablissement = column[Option[String]]("distributionspecialeetablissement")
  def codeCommuneEtablissement = column[Option[String]]("codecommuneetablissement")
  def codeCedexEtablissement = column[Option[String]]("codecedexetablissement")
  def libelleCedexEtablissement = column[Option[String]]("libellecedexetablissement")
  def denominationUsuelleEtablissement = column[Option[String]](DENOMINATION_USUELLE_ETABLISSEMENT)
  def enseigne1Etablissement = column[Option[String]]("enseigne1etablissement")
  def activitePrincipaleEtablissement = column[String]("activiteprincipaleetablissement")
  def etatAdministratifEtablissement = column[Option[String]]("etatadministratifetablissement")

  def * = (
    id,
    siret,
    siren,
    dateDernierTraitementEtablissement,
    etablissementSiege,
    complementAdresseEtablissement,
    numeroVoieEtablissement,
    indiceRepetitionEtablissement,
    typeVoieEtablissement,
    libelleVoieEtablissement,
    codePostalEtablissement,
    libelleCommuneEtablissement,
    libelleCommuneEtrangerEtablissement,
    distributionSpecialeEtablissement,
    codeCommuneEtablissement,
    codeCedexEtablissement,
    libelleCedexEtablissement,
    denominationUsuelleEtablissement,
    enseigne1Etablissement,
    activitePrincipaleEtablissement,
    etatAdministratifEtablissement
  ) <> (CompanyData.tupled, CompanyData.unapply)
}

object CompanyDataTable {
  val DENOMINATION_USUELLE_ETABLISSEMENT = "denominationusuelleetablissement"
  val table = TableQuery[CompanyDataTable]
}
