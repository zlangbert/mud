package mud

import java.util.UUID

import akka.actor._
import mud.Room.RoomInfo

import scala.collection.mutable

class Room(info: RoomInfo) extends Actor {

  /**
    * Players currently in the room
    *
    * @note I think Buffer's `-=` is O(n). A different data structure would
    *       probably be better for this.
    */
  val players = mutable.Buffer[ActorRef]()

  import Room.Protocol._
  def receive =
    protocolReceive orElse
      globalEventReceive

  val globalEventReceive: Receive = {
    case GlobalEvents.PlayerLeftServer(player) => players -= player
  }

  val protocolReceive: Receive = {
    case GetInfo => sender ! info
    case PlayerEntered(player) => players += player
    case PlayerLeft(player) => players -= player
  }
}

object Room {

  case class RoomInfo(id: UUID, name: String, description: String, exits: RoomExits)

  case class RoomExits(north: Option[UUID] = None, east: Option[UUID] = None,
                       south: Option[UUID] = None, west: Option[UUID] = None) {
    override def toString: String = {
      val exits = Seq(
        north.map(_ => "north"),
        east.map(_ => "east"),
        south.map(_ => "south"),
        west.map(_ => "west")
      )
      "Exits: " + exits.flatten.mkString(", ")
    }
  }

  object Protocol {
    case object GetInfo
    case class PlayerEntered(player: ActorRef)
    case class PlayerLeft(player: ActorRef)
  }

}
