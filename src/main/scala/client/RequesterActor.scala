package client

import akka.actor._
import akka.routing.RoundRobinGroup
import client.RequesterActor.RegisterSupervisor
import com.virtuslab.akkaworkshop.PasswordsDistributor._

class RequesterActor extends Actor with ActorLogging {

  var supervisorSet = Set.empty[(ActorRef, Int)]
  val name = "Cesar"

  def createRouter = {
    val addresses = supervisorSet.map(_._1.path.toString)
    context.actorOf(RoundRobinGroup(addresses).props(), s"router_${supervisorSet.size}")
  }

  override def preStart() = {
    val remoteIp = "headquarters"
    val remotePort = 9552
    val remoteSelection = context.actorSelection(s"akka.tcp://application@$remoteIp:$remotePort/user/PasswordsDistributor")
    remoteSelection ! Register(name)
  }

  override def receive: Receive = starting

  def starting: Receive = {
    case Registered(token) =>
      log.info(s"Registered with token $token")
      sender() ! SendMeEncryptedPassword(token)

      val router = createRouter
      context.become(registered(token, router, sender()))

      for {
        (supRef, num) <- supervisorSet
        _ <- 0 until num
      } sender() ! SendMeEncryptedPassword(token)

    case RegisterSupervisor(ref, num) =>
      supervisorSet = supervisorSet + (ref -> num)
  }

  def registered(token: String, router: ActorRef, remote: ActorRef): Receive = {

    case RegisterSupervisor(ref, num) =>
      supervisorSet = supervisorSet + (ref -> num)
      for (_ <- 0 until num) remote ! SendMeEncryptedPassword(token)
      context.stop(router)
      val newRouter = createRouter
      context.become(registered(token, newRouter, remote))

    case encryptedPassword: EncryptedPassword =>
      router ! encryptedPassword

    case ValidateDecodedPassword(_, encrypted, decrypted) =>
      remote ! ValidateDecodedPassword(token, encrypted, decrypted)

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"Password $decryptedPassword was decrypted successfully")
      remote ! SendMeEncryptedPassword(token)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"Password $decryptedPassword was not decrypted correctly, should be $correctPassword")
      remote ! SendMeEncryptedPassword(token)
  }

}

object RequesterActor {

  def props = Props[RequesterActor]

  case class RegisterSupervisor(actorRef: ActorRef, workerNum: Int)

}