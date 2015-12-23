package mud

import akka.actor._
import akka.util.ByteString
import mud.Player.Protocol.{GetRoom, ChangeRooms}
import mud.net.NetProtocol

/**
  * A player in the game
  * @param netHandler The associated connection handler that sends and
  *                   receives [[mud.net.NetProtocol]] messages
  * @param world The game world
  */
class Player(netHandler: ActorRef, world: ActorRef)
  extends Actor with ActorLogging {

  import Player._

  /**
    * The player state
    */
  var state = State(Actor.noSender)

  /**
    * Accepts commands and decides what to do with them
    */
  val commandHandler = context.actorOf(Props(new CommandHandler))

  override def preStart(): Unit = {
    world ! Events.PlayerJoinedServer(self)
  }

  override def postStop(): Unit = {
    world ! Events.PlayerLeftServer(self)
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
      val command = data.utf8String.replaceAll("""\R""", "")
      commandHandler ! command
    case msg: NetProtocol.Send =>
      netHandler ! msg
  }

  val protocolReceive: Receive = {
    case GetRoom => sender() ! state.room
    case ChangeRooms(room) => state = state.copy(room = room)
  }

  val errorReceive: Receive = {
    case Status.Failure(e) =>
      log.error(e, "Player error")
      netHandler ! NetProtocol.Send(ByteString("\nThere was an error processing your request\n\n"))
  }
}

object Player {

  case class State(room: ActorRef)

  object Protocol {

    case object GetRoom
    case class ChangeRooms(room: ActorRef)
  }

}
