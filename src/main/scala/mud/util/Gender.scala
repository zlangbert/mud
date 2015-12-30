package mud.util

object Gender extends Enumeration {
  type Gender = Value
  val Male, Female = Value

  def fromString(s: String): Option[Gender] = s match {
    case "m" | "M" => Some(Male)
    case "f" | "F" => Some(Female)
    case _ => None
  }
}
