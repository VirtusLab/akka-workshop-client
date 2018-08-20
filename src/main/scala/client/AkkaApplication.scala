package client

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout

object AkkaApplication extends App {

  implicit val timeout = Timeout(10.seconds)

  implicit val system = ActorSystem("RequesterSystem")

  system.actorOf(RequesterActor.props(new PasswordClient()), name = "requester")

}
