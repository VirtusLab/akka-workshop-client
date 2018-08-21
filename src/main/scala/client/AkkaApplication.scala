package client

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._

object AkkaApplication extends App {

  implicit val timeout = Timeout(10.seconds)

  implicit val system = ActorSystem("RequesterSystem")

  system.actorOf(RequesterActor.props(new PasswordClient()), name = "requester")

}
