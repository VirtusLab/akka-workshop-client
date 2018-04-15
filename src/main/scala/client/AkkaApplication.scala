package client

import akka.actor.{ActorSystem, Props}

object AkkaApplication extends App {

  val system = ActorSystem("RequesterSystem")

  val requesterActor = system.actorOf(Props(new RequesterActorSingleton("Krzysiek")), name = "requester")


  /*system.actorOf(ClusterSingletonManager.props(
    singletonProps = RequesterActor.props,
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "consumer"
  )

  val singleton = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/consumer",
    settings = ClusterSingletonProxySettings(system)),
    name = "consumerProxy"
  )*/

  system.actorOf(Props(classOf[Supervisor], requesterActor))

}
