package mud

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel}

import akka.actor.{Actor, ActorLogging, PoisonPill}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClientActor(channel: SocketChannel) extends Actor with ActorLogging {

  import ClientActor._
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
                client.write(buffer)
                self ! Listen
            }
          }
        }
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