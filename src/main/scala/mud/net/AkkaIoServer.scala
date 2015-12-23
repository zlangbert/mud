package mud.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import mud.{Player, World}

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
    IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 1234))
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
      val handler = context.actorOf(Props(new AkkaIoHandler(remote, connection, world)))
      connection ! Register(handler)
  }
}

private class AkkaIoHandler(address: InetSocketAddress, connection: ActorRef, world: ActorRef)
  extends Actor with ActorLogging {

  import Tcp._

  val player = context.actorOf(Props(new Player(self, world)))

  def receive = {
    case PeerClosed =>
      log.info(s"Client disconnected: $address")
      context stop self

    // incoming data from the client
    case Received(data) =>
      player ! NetProtocol.Received(data)

    // data from the server being sent to the client
    case NetProtocol.Send(data) =>
      connection ! Write(data)
  }
}

