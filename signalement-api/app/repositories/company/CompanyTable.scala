package repositories.company

import models.Address
import models.Company
import repositories.DatabaseTable
import slick.lifted.TableQuery
import utils.Constants.Departments
import utils.SIRET
import repositories.PostgresProfile.api._

import java.util.UUID
import java.time.OffsetDateTime

class CompanyTable(tag: Tag) extends DatabaseTable[Company](tag, "companies") {
  def siret = column[SIRET]("siret", O.Unique)
  def creationDate = column[OffsetDateTime]("creation_date")
  def name = column[String]("name")
  def streetNumber = column[Option[String]]("street_number")
  def street = column[Option[String]]("street")
  def addressSupplement = column[Option[String]]("address_supplement")
  def city = column[Option[String]]("city")
  def postalCode = column[Option[String]]("postal_code")
  def department = column[Option[String]]("department")
  def activityCode = column[Option[String]]("activity_code")
  def isHeadOffice = column[Boolean]("is_headoffice")
  def isOpen = column[Boolean]("is_open")

  type CompanyTuple = (
      UUID,
      SIRET,
      OffsetDateTime,
      String,
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Option[String],
      Boolean,
      Boolean
  )

  def constructCompany: CompanyTuple => Company = {
    case (
          id,
          siret,
          creationDate,
          name,
          streetNumber,
          street,
          addressSupplement,
          postalCode,
          city,
          _,
          activityCode,
          isHeadOffice,
          isOpen
        ) =>
      Company(
        id = id,
        siret = siret,
        creationDate = creationDate,
        name = name,
        address = Address(
          number = streetNumber,
          street = street,
          addressSupplement = addressSupplement,
          postalCode = postalCode,
          city = city
        ),
        activityCode = activityCode,
        isHeadOffice = isHeadOffice,
        isOpen = isOpen
      )
  }

  def extractCompany: PartialFunction[Company, CompanyTuple] = {
    case Company(
          id,
          siret,
          creationDate,
          name,
          address,
          activityCode,
          isHeadOffice,
          isOpen
        ) =>
      (
        id,
        siret,
        creationDate,
        name,
        address.number,
        address.street,
        address.addressSupplement,
        address.postalCode,
        address.city,
        address.postalCode.flatMap(Departments.fromPostalCode),
        activityCode,
        isHeadOffice,
        isOpen
      )
  }

  def * = (
    id,
    siret,
    creationDate,
    name,
    streetNumber,
    street,
    addressSupplement,
    postalCode,
    city,
    department,
    activityCode,
    isHeadOffice,
    isOpen
  ) <> (constructCompany, extractCompany.lift)
}

object CompanyTable {
  val table = TableQuery[CompanyTable]
}
