package mud

import java.util.UUID

import akka.actor._
import mud.Room.RoomInfo

class World extends Actor with ActorLogging {

  val startingRoomId = UUID.randomUUID()
  val rooms = Map(
    startingRoomId -> context.actorOf(Props(new Room(RoomInfo("Test Room 1", "This is test room 1."))))
  )

  def receive = {
    case Events.PlayerJoinedServer(player) =>
      val room = rooms(startingRoomId)
      player ! Player.Protocol.ChangeRooms(room)
      room ! Room.Protocol.PlayerJoined(player)
    case Events.PlayerLeftServer(player) =>
      val room = rooms(startingRoomId)
      room ! Room.Protocol.PlayerJoined(player)
  }
}
