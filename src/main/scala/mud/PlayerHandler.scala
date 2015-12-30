package mud

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp
import mud.PlayerHandler._
import mud.net.NetProtocol
import mud.util.Gender
import mud.util.Gender.Gender

class PlayerHandler(address: InetSocketAddress, connection: ActorRef, world: ActorRef)
  extends FSM[State, Data] with ActorLogging {

  import PlayerHandler._

  startWith(LoggingIn, EmptyData)

  when(LoggingIn) {
    case Event(Tcp.Received(data), _) =>
      val name = NetProtocol.sanitize(data)

      val message = NetProtocol.prepareResponse(
        """
          |Gender? (m/f)
        """.stripMargin)
      self ! message

      goto(CreatingCharacter) using CreationData(name, None)
  }

  when(CreatingCharacter) {

    case Event(Tcp.Received(data), d@CreationData(_, None)) =>
      Gender.fromString(NetProtocol.sanitize(data)) match {
        case g@Some(gender) =>
          val data = d.copy(gender = g)
          val player = context.actorOf(Props(new Player(self, world, data)))
          goto(Playing) using PlayerData(player)
        case _ =>
          val message =
            """
              |'m' or 'f' please
            """.stripMargin
          self ! NetProtocol.prepareResponse(message)
          stay
      }
  }

  when(Playing) {
    case Event(Tcp.Received(data), PlayerData(player)) =>
      player ! NetProtocol.Received(data)
      stay
  }

  whenUnhandled {
    case Event(NetProtocol.SendToClient(data), _) =>
      connection ! Tcp.Write(data)
      stay
    case Event(NetProtocol.Disconnect, _) =>
      connection ! Tcp.Close
      stay
    case Event(Tcp.Closed, _) | Event(Tcp.PeerClosed, _) =>
      log.info(s"Client disconnected: $address")
      context stop self
      stay
  }

  onTransition {
    case _ -> LoggingIn =>
      val message = NetProtocol.prepareResponse(
        """
          |Welcome to MUD!
          |
          |Name?
        """.stripMargin)
      self ! message
  }

  initialize()
}

object PlayerHandler {

  sealed trait State
  case object LoggingIn extends State
  case object CreatingCharacter extends State
  case object Playing extends State

  sealed trait Data
  case object EmptyData extends Data
  case class CreationData(name: String, gender: Option[Gender]) extends Data
  case class PlayerData(player: ActorRef) extends Data
}