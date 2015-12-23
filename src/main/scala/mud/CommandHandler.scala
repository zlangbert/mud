package mud

import akka.actor._
import akka.util.{Timeout, ByteString}
import mud.Room.RoomInfo
import mud.net.NetProtocol
import akka.pattern.ask
import akka.pattern.pipe
import scala.concurrent.duration._

class CommandHandler extends Actor {

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
          """.stripMargin
        prepareResponse(response)
      }) pipeTo player

    case c =>
      sender() ! NetProtocol.Send(ByteString(s"\nInvalid command: $c\n\n"))
  }

  def prepareResponse(response: String): NetProtocol.Send = {
    val data = ByteString(response + "\n")
    NetProtocol.Send(data)
  }
}
