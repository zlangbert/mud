package mud.net

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import mud.{PlayerHandler, World}

/**
  * @see [[ServerBackend.AkkaIo]]
  */
class AkkaIoServer(host: String, port: Int) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  /**
    * The game world
    */
  val world = context.actorOf(Props(new World))

  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))
  }

  override def postStop(): Unit = {
    IO(Tcp) ! Unbind
  }

  def receive = {
    case Bound(localAddress) =>
      log.info(s"Server bound to $localAddress")
    case CommandFailed(b: Bind) =>
      log.error(s"Server failed to bind to ${b.localAddress}")
      context stop self

    case Connected(remote, local) =>
      log.info(s"Client connected: $remote")
      val connection = sender()
      val handler = context.actorOf(Props(new PlayerHandler(remote, connection, world)))
      connection ! Register(handler)
  }
}