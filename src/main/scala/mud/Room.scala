package mud

import java.util.UUID

import akka.actor._
import akka.util.Timeout
import mud.Player.PlayerInfo
import mud.Room.RoomInfo
import akka.pattern.ask
import mud.net.NetProtocol
import scala.concurrent.duration._

import scala.collection.mutable

class Room(info: RoomInfo) extends Actor {

  import context.dispatcher

  implicit val timeout: Timeout = 5.seconds

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

    case LocalMessage(speaker, message) =>
      for {
        info <- (speaker ? Player.Protocol.GetInfo).mapTo[PlayerInfo]
      } {
        val speakerMsg = NetProtocol.prepareResponse(s"You say '$message'\n")
        val othersMsg = NetProtocol.prepareResponse(s"${info.name} says '$message'\n")
        players.foreach {
          case p if p != speaker => p ! othersMsg
          case s => s ! speakerMsg
        }
      }
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
    case class LocalMessage(speaker: ActorRef, message: String)
  }

}
