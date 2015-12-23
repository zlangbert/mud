package mud

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, Timeout}
import mud.Room.RoomInfo
import mud.net.NetProtocol
import mud.util.Direction

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * A player in the game
  * @param netHandler The associated connection handler that sends and
  *                   receives [[mud.net.NetProtocol]] messages
  * @param world The game world
  */
class Player(netHandler: ActorRef, world: ActorRef)
  extends Actor with ActorLogging {

  import Player._
  import context.dispatcher


  /**
    * The player state
    */
  var state = State(PlayerInfo("Player 1"), Actor.noSender)

  /**
    * Accepts commands and decides what to do with them
    */
  val commandHandler = context.actorOf(Props(new CommandHandler(world)))

  implicit val timeout: Timeout = 5.seconds

  override def preStart(): Unit = {
    context.system.eventStream.publish(GlobalEvents.PlayerJoinedServer(self))
  }

  override def postStop(): Unit = {
    context.system.eventStream.publish(GlobalEvents.PlayerLeftServer(self))
  }

  /**
    * [[Receive]]
    */
  def receive =
    netReceive orElse
    protocolReceive orElse
    errorReceive

  /**
    * Handles data coming from the tcp connection or
    * data from another actor going back to the client
    */
  val netReceive: Receive = {
    case NetProtocol.Received(data) =>
      val input = data.utf8String.replaceAll("""\R""", "")
      val command = Commands.parse(input)
      commandHandler ! command
    case msg: NetProtocol.Send =>
      netHandler ! msg
  }

  import Player.Protocol._
  val protocolReceive: Receive = {

    case GetInfo => sender() ! state.info

    case GetRoom => sender() ! state.room
    case ChangeRooms(room) => state = state.copy(room = room)

    case Move(direction) => move(direction)
  }

  val errorReceive: Receive = {
    case Status.Failure(e) =>
      log.error(e, "Player error")
      netHandler ! NetProtocol.Send(ByteString("\nThere was an error processing your request\n\n"))
  }

  def move(direction: Direction.Direction): Unit = {
    (for {
      currentRoom <- (self ? Player.Protocol.GetRoom).mapTo[ActorRef]
      currentInfo <- (currentRoom ? Room.Protocol.GetInfo).mapTo[RoomInfo]
      response <- checkExit(currentRoom, currentInfo)
    } yield response) pipeTo self

    def checkExit(currentRoom: ActorRef, currentInfo: RoomInfo): Future[NetProtocol.Send] = {

      import Direction._
      val maybeExit = direction match {
        case North => currentInfo.exits.north
        case East => currentInfo.exits.east
        case South => currentInfo.exits.south
        case West => currentInfo.exits.west
      }

      maybeExit.map { exit =>
        (world ? World.Protocol.GetRoom(exit)).mapTo[ActorRef].map { target =>
          self ! Player.Protocol.ChangeRooms(target)
          currentRoom ! Room.Protocol.PlayerLeft(self)
          target ! Room.Protocol.PlayerEntered(self)
          commandHandler ! Commands.Look
          NetProtocol.SendEmpty
        }
      }.getOrElse(Future.successful(NetProtocol.prepareResponse("No exit there!\n")))
    }
  }
}

object Player {

  case class PlayerInfo(name: String)

  case class State(info: PlayerInfo, room: ActorRef)

  object Protocol {

    case object GetInfo

    case object GetRoom
    case class ChangeRooms(room: ActorRef)

    case class Move(direction: Direction.Direction)
  }

}
