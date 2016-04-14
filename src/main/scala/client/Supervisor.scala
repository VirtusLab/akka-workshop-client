package client

import akka.actor.{ActorLogging, ActorRef, Actor, AllForOneStrategy}
import akka.actor.SupervisorStrategy.Restart
import akka.routing.RoundRobinPool
import client.RequesterActor.RegisterSupervisor
import com.virtuslab.akkaworkshop.PasswordsDistributor.EncryptedPassword

class Supervisor(requesterActor : ActorRef) extends Actor with ActorLogging{

  val workersNumber = 10
  val restartingStrategy = AllForOneStrategy() { case _: Exception => Restart }
  val workers = context.actorOf(RoundRobinPool(workersNumber, supervisorStrategy = restartingStrategy).props(Worker.props))

  override def preStart(): Unit = {
    requesterActor ! RegisterSupervisor(self, workersNumber)
  }

  override def receive: Receive = {
    case EncryptedPassword(encryptedPassword) =>
      workers forward List(encryptedPassword)
  }
}