package repositories

import models.website.Website
import models.website.IdentificationStatus
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import utils.AppSpec
import utils.Fixtures
import utils.TestApp

import scala.concurrent.Await
import scala.concurrent.duration._

class WebsiteRepositorySpec(implicit ee: ExecutionEnv) extends Specification with AppSpec with FutureMatchers {

  val (app, components) = TestApp.buildApp(
    None
  )

  lazy val companyRepository = components.companyRepository
  lazy val websiteRepository = components.websiteRepository

  val defaultCompany = Fixtures.genCompany.sample.get
  val marketplaceCompany = Fixtures.genCompany.sample.get
  val pendingCompany = Fixtures.genCompany.sample.get

  val defaultWebsite = Fixtures
    .genWebsite()
    .sample
    .get
    .copy(
      companyCountry = None,
      companyId = Some(defaultCompany.id),
      identificationStatus = IdentificationStatus.Identified
    )
  val marketplaceWebsite =
    Fixtures
      .genWebsite()
      .sample
      .get
      .copy(
        companyCountry = None,
        companyId = Some(marketplaceCompany.id),
        identificationStatus = IdentificationStatus.Identified,
        isMarketplace = true
      )

  val pendingWebsite = Fixtures
    .genWebsite()
    .sample
    .get
    .copy(
      companyCountry = None,
      companyId = Some(pendingCompany.id),
      identificationStatus = IdentificationStatus.NotIdentified
    )

  val newHost = Fixtures.genWebsiteURL.sample.get.getHost.get

  override def setupData() =
    Await.result(
      for {
        _ <- companyRepository.getOrCreate(defaultCompany.siret, defaultCompany)
        _ <- companyRepository.getOrCreate(marketplaceCompany.siret, marketplaceCompany)
        _ <- companyRepository.getOrCreate(pendingCompany.siret, pendingCompany)
        _ <- websiteRepository.validateAndCreate(defaultWebsite)
        _ <- websiteRepository.validateAndCreate(marketplaceWebsite)
        _ <- websiteRepository.validateAndCreate(pendingWebsite)
      } yield (),
      Duration.Inf
    )

  def is = s2"""

 This is a specification to check the WebsiteRepositoryInterface

 Searching by URL should
    retrieve default website                                            $e1
    retrieve marketplace website                                        $e3
    not retrieve pending website                                        $e2

 Adding new website on company should
    if the website is already define for the company, return existing website       $e5
    else add new website with pending kind                                          $e7
 """

  def e1 = websiteRepository.searchCompaniesByUrl(s"http://${defaultWebsite.host}") must beEqualTo(
    Seq((defaultWebsite, defaultCompany))
  ).await
  def e2 = websiteRepository.searchCompaniesByUrl(
    s"http://${pendingWebsite.host}"
  ) must beEqualTo(Seq.empty).await

  def e3 = websiteRepository.searchCompaniesByUrl(s"http://${marketplaceWebsite.host}") must beEqualTo(
    Seq((marketplaceWebsite, marketplaceCompany))
  ).await

  def e5 = websiteRepository.validateAndCreate(
    Website(host = defaultWebsite.host, companyCountry = None, companyId = Some(defaultCompany.id))
  ) must beEqualTo(
    defaultWebsite
  ).await
  def e7 = {
    val newWebsite =
      websiteRepository.validateAndCreate(
        Website(host = newHost, companyCountry = None, companyId = Some(defaultCompany.id))
      )
    newWebsite
      .map(w => (w.host, w.companyId, w.identificationStatus)) must beEqualTo(
      (newHost, Some(defaultCompany.id), IdentificationStatus.NotIdentified)
    ).await
  }
}
