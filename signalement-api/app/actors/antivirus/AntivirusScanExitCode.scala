package actors.antivirus

import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry

/** see https://linux.die.net/man/1/clamdscan for more information
  *
  * @param value
  *   0 : No virus found. 1 : Virus(es) found. 2 : An error occured.
  */
sealed abstract class AntivirusScanExitCode(val value: Int) extends IntEnumEntry

object AntivirusScanExitCode extends IntEnum[AntivirusScanExitCode] {
  case object NoVirusFound extends AntivirusScanExitCode(0)
  case object VirusFound extends AntivirusScanExitCode(1)
  case object ErrorOccured extends AntivirusScanExitCode(2)
  override def values: IndexedSeq[AntivirusScanExitCode] = findValues
}
