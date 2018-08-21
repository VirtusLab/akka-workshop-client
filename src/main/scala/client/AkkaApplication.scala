package client

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object AkkaApplication extends App {

  implicit val timeout = Timeout(10.seconds)

  implicit val system = ActorSystem("RequesterSystem")

  val remoteIp   = "headquarters"
  val remotePort = 9552

  val remoteServer = system.actorSelection(s"akka.tcp://application@$remoteIp:$remotePort/user/PasswordsDistributor")

  remoteServer
    .resolveOne()
    .map { remoteServerRef =>

    }
    .failed
    .foreach { error =>
      println(s"Unable to establish connection to $remoteIp:$remotePort; ${error.getMessage}")
      system.terminate()
    }

  system.actorOf(RequesterActor.props(new PasswordClient()), name = "requester")

}
