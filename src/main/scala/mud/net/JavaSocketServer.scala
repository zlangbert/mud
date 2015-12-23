package mud.net

import java.io.ByteArrayOutputStream
import java.net.{InetSocketAddress, ServerSocket, Socket}

import akka.actor._
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * @see [[ServerBackend.JavaSockets]]
  */
class JavaSocketServer(host: String, port: Int) extends Actor with ActorLogging {

  import JavaSocketServer._
  import context.dispatcher

  val server = new ServerSocket()

  val buffer = Array.fill(8192)(0.toByte)

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case msg: Message => msg match {

      case Start =>
        log.info("Starting server...")
        server.bind(new InetSocketAddress(host, port))

        self ! Listen

      case Listen =>
        log.debug("Listening...")
        Future {
          server.accept()
        }.onComplete {
          case Success(socket) => self ! Process(socket)
          case Failure(e) => throw e
        }

      case Process(socket) =>
        log.info(s"Client connected: ${socket.getRemoteSocketAddress}")
        val client = context.actorOf(Props(new JavaSocketClient(socket)))
        client ! JavaSocketClient.Listen

        self ! Listen

      case Stop =>
        self ! PoisonPill
    }
  }

  override def postStop(): Unit = {
    log.info("Stopping server...")
    server.close()
  }
}

object JavaSocketServer {

  sealed trait Message
  case object Start extends Message
  case object Stop extends Message

  private[JavaSocketServer] case object Listen extends Message
  private[JavaSocketServer] case class Process(socket: Socket) extends Message

}

private class JavaSocketClient(socket: Socket) extends Actor {

  import JavaSocketClient._
  import context.dispatcher

  val is = socket.getInputStream
  val os = socket.getOutputStream

  //TODO: this does not handle the client closing the connection

  def receive = {

    case Listen =>
      if (is.available() > 0) {
        self ! Read
      } else {
        context.system.scheduler.scheduleOnce(50.millis, self, Listen)
      }

    case Read =>
      self ! Listen
      var n = 0
      val buffer = Array.ofDim[Byte](8192)
      val bos = new ByteArrayOutputStream()
      n = is.read(buffer)
      bos.write(buffer)
      bos.flush()

      val command = ByteString(bos.toByteArray)
      self ! NetProtocol.Send(command)

    case NetProtocol.Send(data) =>
      val bb = data.asByteBuffer
      val bytes = Array.ofDim[Byte](bb.remaining())
      bb.get(bytes)
      os.write(bytes)
      os.flush()
  }

  override def postStop(): Unit = {
    socket.close()
  }
}

private object JavaSocketClient {

  case object Listen
  case object Read
}
