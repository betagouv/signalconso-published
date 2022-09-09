package utils

import play.api.libs.json._
import repositories.PostgresProfile.api._

sealed case class Country(code: String, name: String, european: Boolean = false, transfer: Boolean = false)

object Country {

  val Afghanistan = Country("AF", "Afghanistan")
  val AfriqueDuSud = Country("ZA", "Afrique du Sud")
  val Albanie = Country("AL", "Albanie")
  val Algerie = Country("DZ", "Algerie")
  val Allemagne = Country("DE", "Allemagne", european = true)
  val Andorre = Country("AD", "Andorre", transfer = true)
  val Angola = Country("AO", "Angola")
  val AntiguaEtBarbuda = Country("AG", "Antigua-et-Barbuda")
  val ArabieSaoudite = Country("SA", "Arabie saoudite")
  val Argentine = Country("AR", "Argentine")
  val Armenie = Country("AM", "Arménie")
  val Australie = Country("AU", "Australie")
  val Autriche = Country("AT", "Autriche", european = true)
  val Azerbaidjan = Country("AZ", "Azerbaïdjan")
  val Bahamas = Country("BS", "Bahamas")
  val Bahrein = Country("BH", "Bahreïn")
  val Bangladesh = Country("BD", "Bangladesh")
  val Barbade = Country("BB", "Barbade")
  val Belgique = Country("BE", "Belgique", european = true)
  val Belize = Country("BZ", "Bélize")
  val Benin = Country("BJ", "Bénin")
  val Bhoutan = Country("BT", "Bhoutan")
  val Bielorussie = Country("BY", "Biélorussie")
  val Birmanie = Country("MM", "Birmanie")
  val Bolivie = Country("BO", "Bolivie")
  val BosnieHerzegovine = Country("BA", "Bosnie-Herzégovine")
  val Botswana = Country("BW", "Botswana")
  val Bresil = Country("BR", "Brésil")
  val Brunei = Country("BN", "Brunei")
  val Bulgarie = Country("BG", "Bulgarie", european = true)
  val Burkina = Country("BF", "Burkina")
  val Burundi = Country("BI", "Burundi")
  val Cambodge = Country("KH", "Cambodge")
  val Cameroun = Country("CM", "Cameroun")
  val Canada = Country("CA", "Canada")
  val CapVert = Country("CV", "Cap-Vert")
  val Centrafrique = Country("CF", "Centrafrique")
  val Chili = Country("CL", "Chili")
  val Chine = Country("CN", "Chine")
  val Chypre = Country("CY", "Chypre", european = true)
  val Colombie = Country("CO", "Colombie")
  val Comores = Country("KM", "Comores")
  val Congo = Country("CG", "Congo")
  val RepubliqueDemocratiqueDuCongo = Country("CD", "République démocratique du Congo")
  val IlesCook = Country("CK", "Îles Cook")
  val CoreeDuNord = Country("KP", "Corée du Nord")
  val CoreeDuSud = Country("KR", "Corée du Sud")
  val CostaRica = Country("CR", "Costa Rica")
  val CoteDIvoire = Country("CI", "Côte d'Ivoire")
  val Croatie = Country("HR", "Croatie", european = true)
  val Cuba = Country("CU", "Cuba")
  val Danemark = Country("DK", "Danemark", european = true)
  val Djibouti = Country("DJ", "Djibouti")
  val RepubliqueDominicaine = Country("DO", "République dominicaine")
  val Dominique = Country("DM", "Dominique")
  val Egypte = Country("EG", "Égypte")
  val EmiratsArabesUnis = Country("AE", "Émirats arabes unis")
  val Equateur = Country("EC", "Équateur")
  val Erythree = Country("ER", "Érythrée")
  val Espagne = Country("ES", "Espagne", european = true)
  val Estonie = Country("EE", "Estonie", european = true)
  val Eswatini = Country("SZ", "Eswatini")
  val EtatsUnis = Country("US", "États-Unis")
  val Ethiopie = Country("ET", "Éthiopie")
  val Fidji = Country("FJ", "Fidji")
  val Finlande = Country("FI", "Finlande", european = true)
  val France = Country("FR", "France", european = true)
  val Gabon = Country("GA", "Gabon")
  val Gambie = Country("GM", "Gambie")
  val Georgie = Country("GE", "Géorgie")
  val Ghana = Country("GH", "Ghana")
  val Grece = Country("GR", "Grèce", european = true)
  val Grenade = Country("GD", "Grenade")
  val Guatemala = Country("GT", "Guatémala")
  val Guinee = Country("GN", "Guinée")
  val GuineeEquatoriale = Country("GQ", "Guinée équatoriale")
  val GuineeBissao = Country("GW", "Guinée-Bissao")
  val Guyana = Country("GY", "Guyana")
  val Haiti = Country("HT", "Haïti")
  val Honduras = Country("HN", "Honduras")
  val Hongrie = Country("HU", "Hongrie", european = true)
  val Inde = Country("IN", "Inde")
  val Indonesie = Country("ID", "Indonésie")
  val Irak = Country("IQ", "Irak")
  val Iran = Country("IR", "Iran")
  val Irlande = Country("IE", "Irlande", european = true)
  val Islande = Country("IS", "Islande", european = true)
  val Israel = Country("IL", "Israël")
  val Italie = Country("IT", "Italie", european = true)
  val Jamaique = Country("JM", "Jamaïque")
  val Japon = Country("JP", "Japon")
  val Jordanie = Country("JO", "Jordanie")
  val Kazakhstan = Country("KZ", "Kazakhstan")
  val Kenya = Country("KE", "Kénya")
  val Kirghizstan = Country("KG", "Kirghizstan")
  val Kiribati = Country("KI", "Kiribati")
  val Kosovo = Country("XK", "Kosovo")
  val Koweit = Country("KW", "Koweït")
  val Laos = Country("LA", "Laos")
  val Lesotho = Country("LS", "Lésotho")
  val Lettonie = Country("LV", "Lettonie", european = true)
  val Liban = Country("LB", "Liban")
  val Liberia = Country("LR", "Libéria")
  val Libye = Country("LY", "Libye")
  val Liechtenstein = Country("LI", "Liechtenstein")
  val Lituanie = Country("LT", "Lituanie", european = true)
  val Luxembourg = Country("LU", "Luxembourg", european = true)
  val MacedoineDuNord = Country("MK", "Macédoine du Nord")
  val Madagascar = Country("MG", "Madagascar")
  val Malaisie = Country("MY", "Malaisie")
  val Malawi = Country("MW", "Malawi")
  val Maldives = Country("MV", "Maldives")
  val Mali = Country("ML", "Mali")
  val Malte = Country("MT", "Malte", european = true)
  val Maroc = Country("MA", "Maroc")
  val IlesMarshall = Country("MH", "Îles Marshall")
  val Maurice = Country("MU", "Maurice")
  val Mauritanie = Country("MR", "Mauritanie")
  val Mexique = Country("MX", "Mexique")
  val Micronesie = Country("FM", "Micronésie")
  val Moldavie = Country("MD", "Moldavie")
  val Monaco = Country("MC", "Monaco")
  val Mongolie = Country("MN", "Mongolie")
  val Montenegro = Country("ME", "Monténégro")
  val Mozambique = Country("MZ", "Mozambique")
  val Namibie = Country("NA", "Namibie")
  val Nauru = Country("NR", "Nauru")
  val Nepal = Country("NP", "Népal")
  val Nicaragua = Country("NI", "Nicaragua")
  val Niger = Country("NE", "Niger")
  val Nigeria = Country("NG", "Nigéria")
  val Niue = Country("NU", "Niue")
  val Norvege = Country("NO", "Norvège", european = true)
  val NouvelleZelande = Country("NZ", "Nouvelle-Zélande")
  val Oman = Country("OM", "Oman")
  val Ouganda = Country("UG", "Ouganda")
  val Ouzbekistan = Country("UZ", "Ouzbékistan")
  val Pakistan = Country("PK", "Pakistan")
  val Palaos = Country("PW", "Palaos")
  val Panama = Country("PA", "Panama")
  val PapouasieNouvelleGuinee = Country("PG", "Papouasie-Nouvelle-Guinée")
  val Paraguay = Country("PY", "Paraguay")
  val PaysBas = Country("NL", "Pays-Bas", european = true)
  val Perou = Country("PE", "Pérou")
  val Philippines = Country("PH", "Philippines")
  val Pologne = Country("PL", "Pologne", european = true)
  val Portugal = Country("PT", "Portugal", european = true)
  val Qatar = Country("QA", "Qatar")
  val Roumanie = Country("RO", "Roumanie", european = true)
  val RoyaumeUni = Country("GB", "Royaume-Uni")
  val Russie = Country("RU", "Russie")
  val Rwanda = Country("RW", "Rwanda")
  val SaintChristopheEtNieves = Country("KN", "Saint-Christophe-et-Niévès")
  val SainteLucie = Country("LC", "Sainte-Lucie")
  val SaintMarin = Country("SM", "Saint-Marin")
  val SaintVincentEtLesGrenadines = Country("VC", "Saint-Vincent-et-les-Grenadines")
  val Salomon = Country("SB", "Salomon")
  val Salvador = Country("SV", "Salvador")
  val Samoa = Country("WS", "Samoa")
  val SaoTomeEtPrincipe = Country("ST", "Sao Tomé-et-Principe")
  val Senegal = Country("SN", "Sénégal")
  val Serbie = Country("RS", "Serbie")
  val Seychelles = Country("SC", "Seychelles")
  val SierraLeone = Country("SL", "Sierra Leone")
  val Singapour = Country("SG", "Singapour")
  val Slovaquie = Country("SL", "Slovaquie", european = true)
  val Slovenie = Country("SI", "Slovénie", european = true)
  val Somalie = Country("SO", "Somalie")
  val Soudan = Country("SD", "Soudan")
  val SoudanDuSud = Country("SS", "Soudan du Sud")
  val SriLanka = Country("LK", "Sri Lanka")
  val Suede = Country("SE", "Suède", european = true)
  val Suisse = Country("CH", "Suisse", transfer = true)
  val Suriname = Country("SR", "Suriname")
  val Syrie = Country("SY", "Syrie")
  val Tadjikistan = Country("TJ", "Tadjikistan")
  val Tanzanie = Country("TZ", "Tanzanie")
  val Tchad = Country("TD", "Tchad")
  val Tchequie = Country("CZ", "Tchéquie", european = true)
  val Thailande = Country("TH", "Thaïlande")
  val TimorOriental = Country("TL", "Timor oriental")
  val Togo = Country("TG", "Togo")
  val Tonga = Country("TO", "Tonga")
  val TriniteEtTobago = Country("TT", "Trinité-et-Tobago")
  val Tunisie = Country("TN", "Tunisie")
  val Turkmenistan = Country("TM", "Turkménistan")
  val Turquie = Country("TR", "Turquie")
  val Tuvalu = Country("TV", "Tuvalu")
  val Ukraine = Country("UA", "Ukraine")
  val Uruguay = Country("UY", "Uruguay")
  val Vanuatu = Country("VU", "Vanuatu")
  val Vatican = Country("VAT", "Vatican")
  val Venezuela = Country("VE", "Vénézuéla")
  val Vietnam = Country("VN", "Vietnam")
  val Yemen = Country("YE", "Yémen")
  val Zambie = Country("ZM", "Zambie")
  val Zimbabwe = Country("ZW", "Zimbabwé")

  val countries = List(
    Afghanistan,
    AfriqueDuSud,
    Albanie,
    Algerie,
    Allemagne,
    Andorre,
    Angola,
    AntiguaEtBarbuda,
    ArabieSaoudite,
    Argentine,
    Armenie,
    Australie,
    Autriche,
    Azerbaidjan,
    Bahamas,
    Bahrein,
    Bangladesh,
    Barbade,
    Belgique,
    Belize,
    Benin,
    Bhoutan,
    Bielorussie,
    Birmanie,
    Bolivie,
    BosnieHerzegovine,
    Botswana,
    Bresil,
    Brunei,
    Bulgarie,
    Burkina,
    Burundi,
    Cambodge,
    Cameroun,
    Canada,
    CapVert,
    Centrafrique,
    Chili,
    Chine,
    Chypre,
    Colombie,
    Comores,
    Congo,
    RepubliqueDemocratiqueDuCongo,
    IlesCook,
    CoreeDuNord,
    CoreeDuSud,
    CostaRica,
    CoteDIvoire,
    Croatie,
    Cuba,
    Danemark,
    Djibouti,
    RepubliqueDominicaine,
    Dominique,
    Egypte,
    EmiratsArabesUnis,
    Equateur,
    Erythree,
    Espagne,
    Estonie,
    Eswatini,
    EtatsUnis,
    Ethiopie,
    Fidji,
    Finlande,
    France,
    Gabon,
    Gambie,
    Georgie,
    Ghana,
    Grece,
    Grenade,
    Guatemala,
    Guinee,
    GuineeEquatoriale,
    GuineeBissao,
    Guyana,
    Haiti,
    Honduras,
    Hongrie,
    Inde,
    Indonesie,
    Irak,
    Iran,
    Irlande,
    Islande,
    Israel,
    Italie,
    Jamaique,
    Japon,
    Jordanie,
    Kazakhstan,
    Kenya,
    Kirghizstan,
    Kiribati,
    Kosovo,
    Koweit,
    Laos,
    Lesotho,
    Lettonie,
    Liban,
    Liberia,
    Libye,
    Liechtenstein,
    Lituanie,
    Luxembourg,
    MacedoineDuNord,
    Madagascar,
    Malaisie,
    Malawi,
    Maldives,
    Mali,
    Malte,
    Maroc,
    IlesMarshall,
    Maurice,
    Mauritanie,
    Mexique,
    Micronesie,
    Moldavie,
    Monaco,
    Mongolie,
    Montenegro,
    Mozambique,
    Namibie,
    Nauru,
    Nepal,
    Nicaragua,
    Niger,
    Nigeria,
    Niue,
    Norvege,
    NouvelleZelande,
    Oman,
    Ouganda,
    Ouzbekistan,
    Pakistan,
    Palaos,
    Panama,
    PapouasieNouvelleGuinee,
    Paraguay,
    PaysBas,
    Perou,
    Philippines,
    Pologne,
    Portugal,
    Qatar,
    Roumanie,
    RoyaumeUni,
    Russie,
    Rwanda,
    SaintChristopheEtNieves,
    SainteLucie,
    SaintMarin,
    SaintVincentEtLesGrenadines,
    Salomon,
    Salvador,
    Samoa,
    SaoTomeEtPrincipe,
    Senegal,
    Serbie,
    Seychelles,
    SierraLeone,
    Singapour,
    Slovaquie,
    Slovenie,
    Somalie,
    Soudan,
    SoudanDuSud,
    SriLanka,
    Suede,
    Suisse,
    Suriname,
    Syrie,
    Tadjikistan,
    Tanzanie,
    Tchad,
    Tchequie,
    Thailande,
    TimorOriental,
    Togo,
    Tonga,
    TriniteEtTobago,
    Tunisie,
    Turkmenistan,
    Turquie,
    Tuvalu,
    Ukraine,
    Uruguay,
    Vanuatu,
    Vatican,
    Venezuela,
    Vietnam,
    Yemen,
    Zambie,
    Zimbabwe
  )

  def fromCode(code: String) =
    countries.find(_.code == code).head

  def fromName(name: String) =
    countries.find(_.name == name).head

  implicit val reads = new Reads[Country] {
    def reads(json: JsValue): JsResult[Country] = json.validate[String].map(fromName(_))
  }
  implicit val writes = Json.writes[Country]

  implicit val CountryColumnType = MappedColumnType.base[Country, String](_.name, Country.fromName(_))

  implicit val countryListColumnType = MappedColumnType.base[List[Country], List[String]](
    _.map(_.name),
    _.map(Country.fromName)
  )
}
