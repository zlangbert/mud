package mud.net

import akka.actor.{ActorSystem, Props}

abstract class ServerBackend(host: String, port: Int) {

  /**
    * Starts the server
    */
  def start(): Unit
}

object ServerBackend {

  /**
    * A server backend based on Akka IO. The low level details are handled by Akka
    */
  class AkkaIo(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      system.actorOf(Props(new AkkaIoServer(host, port)), "server")
    }
  }

  /**
    * A server backend based on java sockets. A main server actor accepts connections
    * and spawns a worker for each client. The client then polls for input
    */
  class JavaSockets(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      val server = system.actorOf(Props(new JavaSocketServer(host, port)), "server")
      server ! JavaSocketServer.Start
    }
  }

  /**
    * A server backend based on java channels. A main server actor accepts connections
    * and spawns a worker actor for each client. The client then listens for new data
    */
  class JavaChannels(host: String, port: Int)(implicit val system: ActorSystem)
    extends ServerBackend(host, port) {

    override def start(): Unit = {
      val server = system.actorOf(Props(new JavaChannelServer(host, port)), "server")
      server ! JavaChannelServer.Start
    }
  }
}
