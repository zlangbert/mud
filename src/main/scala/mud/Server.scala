package mud

import akka.actor.{ActorSystem, Props}

object Server extends App {

  implicit val system = ActorSystem()

  val server = system.actorOf(Props(new ServerActor(1234, "localhost")), "server")
  server ! ServerActor.Start
}
