package mud

import akka.actor.ActorSystem
import mud.net.ServerBackend

object Main extends App {

  val Host = "localhost"
  val Port = 1234

  implicit val system = ActorSystem()

  val server = new ServerBackend.AkkaIo(Host, Port)
  server.start()
}
