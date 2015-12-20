package mud.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

class AkkaIoServer(host: String, port: Int) extends Actor with ActorLogging {

  import Tcp._
  import context.system

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
      val handler = context.actorOf(Props(new AkkaIoHandler(connection)))
      connection ! Register(handler)
  }
}

private class AkkaIoHandler(connection: ActorRef) extends Actor {

  import Tcp._

  def receive = {
    case PeerClosed =>
      context stop self

    case Received(data) =>
      self ! NetProtocol.Send(data)

    case NetProtocol.Send(data) =>
      connection ! Write(data)
  }
}

