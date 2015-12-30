package mud.commands

import mud.util.Direction
import mud.util.Direction._

object Commands {

  sealed trait Command

  case class Invalid(input: String) extends Command

  case object Quit extends Command

  case object Look extends Command

  case class Move(direction: Direction) extends Command

  case class Say(message: String) extends Command

  def parse(input: String): Command = input.split(" ").toList match {

    case "quit" :: Nil => Quit

    case "look" :: Nil => Look

    case "n" :: Nil => Move(Direction.North)
    case "e" :: Nil => Move(Direction.East)
    case "s" :: Nil => Move(Direction.South)
    case "w" :: Nil => Move(Direction.West)

    case "say" :: tail => Say(tail.mkString(" "))

    case c => Invalid(c.mkString(" "))
  }
}
