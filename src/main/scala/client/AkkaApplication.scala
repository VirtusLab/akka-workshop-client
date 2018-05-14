package client

import akka.actor.{ActorSelection, ActorSystem, Props}

object AkkaApplication extends App {

  val system = ActorSystem("RequesterSystem")

  val requesterActor = system.actorOf(Props[RequesterActor], name = "requester")

  // Remote actor can be found in: "akka.tcp://application@<host-name>:9552/user/PasswordsDistributor"
  val selection: ActorSelection = ???

  requesterActor ! selection

}
