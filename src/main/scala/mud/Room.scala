package mud

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
  def receive = {
    case GetInfo => sender ! info
    case PlayerJoined(player) => players += player
    case PlayerLeft(player) => players -= player
  }
}

object Room {

  case class RoomInfo(name: String, description: String)

  object Protocol {
    case object GetInfo
    case class PlayerJoined(player: ActorRef)
    case class PlayerLeft(player: ActorRef)
  }

}
