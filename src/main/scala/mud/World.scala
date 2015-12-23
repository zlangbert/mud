package mud

import java.util.UUID

import akka.actor._
import mud.Room.{RoomExits, RoomInfo}

class World extends Actor with ActorLogging {

  val testRoom1Id = UUID.randomUUID()
  val testRoom2Id = UUID.randomUUID()

  val testRoom1 = RoomInfo(
    id = testRoom1Id,
    name = "Test Room 1",
    description = "This is test room one.",
    RoomExits(
      south = Some(testRoom2Id)
    )
  )
  val testRoom2 = RoomInfo(
    id = testRoom2Id,
    name = "Test Room 2",
    description = "Welcome to test room two",
    RoomExits(
      north = Some(testRoom1Id)
    )
  )

  val rooms = Map(
    testRoom1.id -> context.actorOf(Props(new Room(testRoom1)), testRoom1Id.toString),
    testRoom2.id -> context.actorOf(Props(new Room(testRoom2)), testRoom2Id.toString)
  )


  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[GlobalEvents.GlobalEvent])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  def receive =
    protocolReceive orElse
    globalEventReceive

  val globalEventReceive: Receive = {
    case GlobalEvents.PlayerJoinedServer(player) =>
      val room = rooms(testRoom1.id)
      player ! Player.Protocol.ChangeRooms(room)
      room ! Room.Protocol.PlayerEntered(player)
    case GlobalEvents.PlayerLeftServer(player) =>
  }

  import World.Protocol._
  val protocolReceive: Receive = {
    case GetRoom(id) =>
      sender() ! rooms.getOrElse(id, throw new Exception(s"Tried to get non-existent room"))
  }
}

object World {

  object Protocol {

    case class GetRoom(id: UUID)
  }
}
