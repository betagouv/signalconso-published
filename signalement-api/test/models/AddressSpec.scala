package models

import org.specs2.mutable.Specification

class AddressSpec extends Specification {

  "AddressSpec" should {

    "When all None" in {
      Address(
        number = None,
        street = None,
        addressSupplement = None,
        postalCode = None,
        city = None
      ).isDefined === false
    }

    "When only number is set" in {
      Address(
        number = Some("1"),
        street = None,
        addressSupplement = None,
        postalCode = None,
        city = None
      ).isDefined === true
    }

    "When only postalCode is set" in {
      Address(
        number = None,
        street = None,
        addressSupplement = None,
        postalCode = Some("90100"),
        city = None
      ).isDefined === true
    }

    "When only addressSupplement is set" in {
      Address(
        number = None,
        street = None,
        addressSupplement = Some("test"),
        postalCode = None,
        city = None
      ).isDefined === true
    }

    "toString" in {
      Address(
        number = Some("13"),
        street = Some("AVENUE FELIX FAURE"),
        addressSupplement = Some("1 RUE ARDOINO"),
        postalCode = Some("06500"),
        city = Some("MENTON")
      ).toString must equalTo("13 AVENUE FELIX FAURE - 1 RUE ARDOINO - 06500 MENTON")

      Address(
        number = None,
        street = None,
        addressSupplement = Some("1 RUE ARDOINO"),
        postalCode = Some("06500"),
        city = Some("MENTON")
      ).toString must equalTo("1 RUE ARDOINO - 06500 MENTON")

      Address(
        number = Some("13"),
        street = Some("AVENUE FELIX FAURE"),
        addressSupplement = None,
        postalCode = None,
        city = Some("MENTON")
      ).toString must equalTo("13 AVENUE FELIX FAURE - MENTON")

      Address(
        number = None,
        street = None,
        addressSupplement = None,
        postalCode = None,
        city = None
      ).toString must equalTo("")
    }
  }
}
