package company

import models.Address
import models.Company
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.SIREN
import utils.SIRET

import java.util.UUID

case class CompanyData(
    id: UUID = UUID.randomUUID(),
    siret: SIRET,
    siren: SIREN,
    dateDernierTraitementEtablissement: Option[String] = None,
    etablissementSiege: Option[String] = None, // TODO change after updating table column type
    complementAdresseEtablissement: Option[String] = None,
    numeroVoieEtablissement: Option[String] = None,
    indiceRepetitionEtablissement: Option[String] = None,
    typeVoieEtablissement: Option[String] = None,
    libelleVoieEtablissement: Option[String] = None,
    codePostalEtablissement: Option[String] = None,
    libelleCommuneEtablissement: Option[String] = None,
    libelleCommuneEtrangerEtablissement: Option[String] = None,
    distributionSpecialeEtablissement: Option[String] = None,
    codeCommuneEtablissement: Option[String] = None,
    codeCedexEtablissement: Option[String] = None,
    libelleCedexEtablissement: Option[String] = None,
    denominationUsuelleEtablissement: Option[String] = None,
    enseigne1Etablissement: Option[String] = None,
    activitePrincipaleEtablissement: String,
    etatAdministratifEtablissement: Option[String] = None
) {
  def toAddress: Address = Address(
    number = numeroVoieEtablissement,
    street = Option(
      Seq(
        typeVoieEtablissement.flatMap(typeVoie => TypeVoies.values.find(_._1 == typeVoie).map(_._2.toUpperCase)),
        libelleVoieEtablissement
      ).flatten
    ).filterNot(_.isEmpty).map(_.mkString(" ")),
    postalCode = codePostalEtablissement,
    city = libelleCommuneEtablissement,
    addressSupplement = complementAdresseEtablissement
  )

  def toSearchResult(activityLabel: Option[String], isMarketPlace: Boolean = false) = company.CompanySearchResult(
    siret = siret,
    name = denominationUsuelleEtablissement,
    brand = enseigne1Etablissement.filter(!denominationUsuelleEtablissement.contains(_)),
    isHeadOffice = etablissementSiege.exists(_.toBoolean),
    address = toAddress,
    activityCode = activitePrincipaleEtablissement,
    activityLabel = activityLabel,
    isMarketPlace = isMarketPlace,
    isOpen = etatAdministratifEtablissement.forall {
      case "O" => true
      case "F" => false
      case _   => true
    }
  )
}

case class CompanyActivity(
    code: String,
    label: String
)

case class CompanySearchResult(
    siret: SIRET,
    name: Option[String],
    brand: Option[String],
    isHeadOffice: Boolean,
    address: Address,
    activityCode: String,
    activityLabel: Option[String],
    isMarketPlace: Boolean = false,
    isOpen: Boolean
) {
  def toCompany() = Company(
    siret = siret,
    name = name.getOrElse(""),
    address = address,
    isHeadOffice = isHeadOffice,
    isOpen = isOpen,
    activityCode = Some(activityCode)
  )
}

object CompanySearchResult {
  implicit val format: OFormat[CompanySearchResult] = Json.format[CompanySearchResult]
}

object TypeVoies {
  val values = Seq(
    ("ALL", "Allée"),
    ("AV", "Avenue"),
    ("BD", "Boulevard"),
    ("CAR", "Carrefour"),
    ("CHE", "Chemin"),
    ("CHS", "Chaussée"),
    ("CITE", "Cité"),
    ("COR", "Corniche"),
    ("CRS", "Cours"),
    ("DOM", "Domaine"),
    ("DSC", "Descente"),
    ("ECA", "Ecart"),
    ("ESP", "Esplanade"),
    ("FG", "Faubourg"),
    ("GR", "Grande Rue"),
    ("HAM", "Hameau"),
    ("HLE", "Halle"),
    ("IMP", "Impasse"),
    ("LD", "Lieu dit"),
    ("LOT", "Lotissement"),
    ("MAR", "Marché"),
    ("MTE", "Montée"),
    ("PAS", "Passage"),
    ("PL", "Place"),
    ("PLN", "Plaine"),
    ("PLT", "Plateau"),
    ("PRO", "Promenade"),
    ("PRV", "Parvis"),
    ("QUA", "Quartier"),
    ("QUAI", "Quai"),
    ("RES", "Résidence"),
    ("RLE", "Ruelle"),
    ("ROC", "Rocade"),
    ("RPT", "Rond Point"),
    ("RTE", "Route"),
    ("RUE", "Rue"),
    ("SEN", "Sente - Sentier"),
    ("SQ", "Square"),
    ("TPL", "Terre-plein"),
    ("TRA", "Traverse"),
    ("VLA", "Villa"),
    ("VLGE", "Village")
  )

  def getByShortName(shortName: String): Option[String] =
    values.find(_._1 == shortName).map(_._2.toUpperCase)
}
