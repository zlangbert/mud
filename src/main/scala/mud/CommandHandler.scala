package mud

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.{ByteString, Timeout}
import mud.Room.RoomInfo
import mud.net.NetProtocol

import scala.concurrent.duration._

class CommandHandler(world: ActorRef) extends Actor {

  import context.dispatcher

  implicit val timeout: Timeout = 5.seconds

  // assume sender is a player
  def receive = {

    case Commands.Look =>
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

    case Commands.Move(direction) =>
      sender() ! Player.Protocol.Move(direction)

    case Commands.Say(message) =>
      val player = sender()
      (player ? Player.Protocol.GetRoom).mapTo[ActorRef]
        .foreach(_ ! Room.Protocol.LocalMessage(player, message))

    case Commands.Invalid(input) =>
      sender() ! NetProtocol.Send(ByteString(s"\nInvalid command: $input\n\n"))
  }
}
