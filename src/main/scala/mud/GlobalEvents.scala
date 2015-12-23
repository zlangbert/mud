package mud

import akka.actor.ActorRef

object GlobalEvents {

  sealed trait GlobalEvent

  case class PlayerJoinedServer(player: ActorRef) extends GlobalEvent
  case class PlayerLeftServer(player: ActorRef) extends GlobalEvent
}
