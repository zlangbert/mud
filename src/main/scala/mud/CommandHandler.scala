package mud

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, Timeout}
import mud.Room.RoomInfo
import mud.net.NetProtocol
import mud.util.Direction

import scala.concurrent.Future
import scala.concurrent.duration._

class CommandHandler(world: ActorRef) extends Actor {

  import context.dispatcher

  implicit val timeout: Timeout = 5.seconds

  // assume sender is a player
  def receive = {

    case "look" =>
      val player = sender()
      (for {
        room <- (player ? Player.Protocol.GetRoom).mapTo[ActorRef]
        info <- (room ? Room.Protocol.GetInfo).mapTo[RoomInfo]
      } yield {
        val response =
          s"""
            |${info.name}
            |${info.description}
            |${info.exits}
          """.stripMargin
        NetProtocol.prepareResponse(response)
      }) pipeTo player

    case "n" =>
      sender() ! Player.Protocol.Move(Direction.North)

    case "e" =>
      sender() ! Player.Protocol.Move(Direction.East)

    case "s" =>
      sender() ! Player.Protocol.Move(Direction.South)

    case "w" =>
      sender() ! Player.Protocol.Move(Direction.West)

    case c =>
      sender() ! NetProtocol.Send(ByteString(s"\nInvalid command: $c\n\n"))
  }
}
