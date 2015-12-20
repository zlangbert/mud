package mud.net

import akka.actor.{ActorSystem, Props}

abstract class ServerBackend(host: String, port: Int) {

  /**
    * Starts the server
    */
  def start(): Unit
}

object ServerBackend {

  class AkkaIo(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      system.actorOf(Props(new AkkaIoServer(host, port)), "server")
    }
  }

  class JavaSockets(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      val server = system.actorOf(Props(new JavaSocketServer(host, port)), "server")
      server ! JavaSocketServer.Start
    }
  }

  class JavaChannels(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      val server = system.actorOf(Props(new JavaChannelServer(host, port)), "server")
      server ! JavaChannelServer.Start
    }
  }
}
