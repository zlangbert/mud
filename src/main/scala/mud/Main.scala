package mud

import akka.actor.ActorSystem
import mud.net.ServerBackend

object Main extends App {

  val Host = "0.0.0.0"
  val Port = 1234

  implicit val system = ActorSystem()

  val server = new ServerBackend.AkkaIo(Host, Port)
  server.start()
}
