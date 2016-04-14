package client

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonProxySettings, ClusterSingletonProxy, ClusterSingletonManagerSettings, ClusterSingletonManager}
import akka.util.Timeout
import scala.concurrent.duration._

object AkkaApplication extends App {

  implicit val timeout = Timeout(10.seconds)

  val system = ActorSystem("RequesterSystem")

  system.actorOf(ClusterSingletonManager.props(
    singletonProps = RequesterActor.props,
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "consumer"
  )

  val singleton = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/consumer",
    settings = ClusterSingletonProxySettings(system)),
    name = "consumerProxy"
  )

  system.actorOf(Props(classOf[Supervisor], singleton))

}
