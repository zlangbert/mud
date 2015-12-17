package mud

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel}

import akka.actor.{ActorLogging, Actor}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClientActor(channel: SocketChannel) extends Actor with ActorLogging {

  import context.dispatcher
  import ClientActor._

  val selector = Selector.open()
  channel.register(selector, SelectionKey.OP_READ)

  val buffer = ByteBuffer.allocate(8192)

  override def receive = {
    case msg: Message => msg match {

      case Listen =>
        Future {
          selector.select()
        } onComplete {
          case Success(_) => self ! Process
          case Failure(e) => throw e
        }

      case Process =>
        selector.selectedKeys().asScala.foreach { key =>
          selector.selectedKeys().remove(key)

          if (key.isValid && key.isReadable) {
            val client = key.channel().asInstanceOf[SocketChannel]
            buffer.clear()
            val n = client.read(buffer)

            buffer.flip()
            client.write(buffer)
          }
        }
        self ! Listen
    }
  }

  override def postStop(): Unit = {
    selector.close()
    channel.close()
  }
}

object ClientActor {
  sealed trait Message
  case object Listen extends Message
  case object Process extends Message
}