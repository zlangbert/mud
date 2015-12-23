package mud

import akka.actor.ActorRef

object Events {

  sealed trait Event

  case class PlayerJoinedServer(player: ActorRef) extends Event
  case class PlayerLeftServer(player: ActorRef) extends Event
}
