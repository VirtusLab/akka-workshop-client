package client

import akka.actor._
import akka.util.Timeout
import scala.concurrent.Await
import scala.util.{Success, Try}
import scala.concurrent.duration._
import akka.pattern.ask

object AkkaApplication extends App {

  implicit val timeout = Timeout(10.seconds)

  val system = ActorSystem("RequesterSystem")

  val remoteIp = "headquarters"
  val remotePort = 9552

  val remoteServer = system.actorSelection(
    s"akka.tcp://application@$remoteIp:$remotePort/user/PasswordsDistributor")

  val remoteServerRef = Try(
    Await
      .result((remoteServer ? Identify("123L")).mapTo[ActorIdentity],
              10.seconds)
      .ref)

  remoteServerRef match {
    case Success(Some(ref)) =>
      system.actorOf(RequesterActor.props(ref), name = "requester")

    case _ =>
      println(s"Unable to establish connection to $remoteIp:$remotePort")
      system.terminate()
  }

}
