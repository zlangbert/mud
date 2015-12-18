package mud

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}

import akka.actor._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ServerActor extends Actor with ActorLogging {

  import context.dispatcher
  import ServerActor._

  val server = ServerSocketChannel.open()
  val selector = Selector.open()
  val readBuffer = ByteBuffer.allocate(8192)

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case msg: Message => msg match {

      case Start =>
        log.info("Starting server...")
        server.bind(new InetSocketAddress("localhost", 1234))
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

            val client = context.actorOf(Props(new ClientActor(channel)))
            client ! ClientActor.Listen
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

object ServerActor {
  sealed trait Message
  case object Start extends Message
  case object Stop extends Message

  private[ServerActor] case object Listen extends Message
  private[ServerActor] case object Process extends Message
}