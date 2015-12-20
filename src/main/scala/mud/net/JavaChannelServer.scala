package mud.net

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}

import akka.actor._
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}

class JavaChannelServer(host: String, port: Int) extends Actor with ActorLogging {

  import JavaChannelServer._
  import context.dispatcher

  val server = ServerSocketChannel.open()
  val selector = Selector.open()

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case msg: Message => msg match {

      case Start =>
        log.info("Starting server...")
        server.bind(new InetSocketAddress(host, port))
        server.configureBlocking(false)
        server.register(selector, SelectionKey.OP_ACCEPT)

        self ! Listen

      case Listen =>
        log.debug("Listening...")
        Future {
          selector.select()
        }.onComplete {
          case Success(_) => self ! Process
          case Failure(e) => throw e
        }

      case Process =>
        val i = selector.selectedKeys().iterator
        while (i.hasNext) {
          val key = i.next()
          i.remove()

          if (key.isValid && key.isAcceptable) {
            log.debug("Accepting...")
            val channel = server.accept()
            channel.configureBlocking(false)
            channel.socket().setTcpNoDelay(false)

            val client = context.actorOf(Props(new JavaChannelClient(channel)))
            client ! JavaChannelClient.Listen
          }
        }
        self ! Listen

      case Stop =>
        self ! PoisonPill
    }
  }

  override def postStop(): Unit = {
    log.info("Stopping server...")
    selector.close()
    server.close()
  }
}

object JavaChannelServer {

  sealed trait Message
  case object Start extends Message
  case object Stop extends Message

  private[JavaChannelServer] case object Listen extends Message
  private[JavaChannelServer] case object Process extends Message

}

private class JavaChannelClient(channel: SocketChannel) extends Actor with ActorLogging {

  import JavaChannelClient._
  import context.dispatcher

  val selector = Selector.open()
  channel.register(selector, SelectionKey.OP_READ)

  val buffer = ByteBuffer.allocate(8192)

  override def receive = {
    case msg: Message => msg match {

      case Listen =>
        Future {
          selector.select()
        } onComplete {
          case Success(0) => self ! Listen
          case Success(n) => self ! Process
          case Failure(e) => throw e
        }

      case Process =>
        val i = selector.selectedKeys().iterator()
        while (i.hasNext) {
          val key = i.next
          i.remove()

          if (key.isValid && key.isReadable) {
            val client = key.channel().asInstanceOf[SocketChannel]
            buffer.clear()
            client.read(buffer) match {
              case -1 => self ! PoisonPill
              case n =>
                buffer.flip()
                self ! Listen

                self ! NetProtocol.Send(ByteString(buffer))
            }
          }
        }
    }

    case NetProtocol.Send(data) =>
      channel.write(data.asByteBuffer)
  }

  override def postStop(): Unit = {
    selector.close()
    channel.close()
  }
}

private object JavaChannelClient {

  sealed trait Message
  case object Listen extends Message
  case object Process extends Message
}
