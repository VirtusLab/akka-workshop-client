package client

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object AkkaApplication extends App {

  implicit val system = ActorSystem("RequesterSystem")

  /*
  val remoteIp   = "headquarters"
  val remotePort = 9552

  val remoteServer = system.actorSelection(s"akka.tcp://application@$remoteIp:$remotePort/user/PasswordsDistributor")

  implicit val timeout = Timeout(10.seconds)
  remoteServer
    .resolveOne()
    .map { remoteServerRef =>
      system.actorOf(RequesterActor.props(remoteServerRef), name = "requester")
    }
    .failed
    .foreach { error =>
      println(s"Unable to establish connection to $remoteIp:$remotePort; ${error.getMessage}")
      system.terminate()
    }
    */


  val passwordClient = new PasswordClient()
  system.actorOf(RequesterActor.props(passwordClient), name = "requester")

}
