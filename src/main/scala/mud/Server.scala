package mud

import akka.actor.{ActorSystem, Props}

import scala.io.StdIn

// command parsing

object Server extends App {

  implicit val system = ActorSystem()

  val server = system.actorOf(Props(new ServerActor(1234, "localhost")), "server")
  server ! ServerActor.Start

  while (StdIn.readLine() != "stop") {}
  println("Shutting down...")
  system.terminate()
}
