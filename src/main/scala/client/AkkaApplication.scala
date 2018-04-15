package client

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}

object AkkaApplication extends App {

  val system = ActorSystem("RequesterSystem")


  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(new RequesterActorSingleton("Krzysiek")),
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "requester"
  )

  val requesterActor = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/requester",
    settings = ClusterSingletonProxySettings(system)),
    name = "requesterProxy"
  )

  system.actorOf(Props(classOf[Supervisor], requesterActor))

}
