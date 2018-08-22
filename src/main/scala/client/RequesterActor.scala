package client

import akka.actor._
import akka.routing.RoundRobinGroup
import client.RequesterActor.RegisterSupervisor
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import akka.pattern.pipe

class RequesterActor extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val remote        = new PasswordClient()(context.system)
  var supervisorSet = Set.empty[(ActorRef, Int)]
  val name          = "Cesar"

  def createRouter = {
    val addresses = supervisorSet.map(_._1.path.toString)
    context.actorOf(RoundRobinGroup(addresses).props(), s"router_${supervisorSet.size}")
  }

  override def preStart() =
    remote.requestToken(name).pipeTo(self)

  override def receive: Receive = starting

  def starting: Receive = {
    case Registered(token) =>
      log.info(s"Registered with token $token")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

      val router = createRouter
      context.become(registered(token, router))

      for {
        (supRef, num) <- supervisorSet
        _             <- 0 until num
      } remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

    case RegisterSupervisor(ref, num) =>
      supervisorSet = supervisorSet + (ref -> num)
  }

  def registered(token: String, router: ActorRef): Receive = {

    case RegisterSupervisor(ref, num) =>
      supervisorSet = supervisorSet + (ref -> num)
      for (_ <- 0 until num) remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
      context.stop(router)
      val newRouter = createRouter
      context.become(registered(token, newRouter))

    case encryptedPassword: EncryptedPassword =>
      router ! encryptedPassword

    case ValidateDecodedPassword(_, encrypted, decrypted) =>
      remote.validatePassword(ValidateDecodedPassword(token, encrypted, decrypted)).pipeTo(self)

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"Password $decryptedPassword was decrypted successfully")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"Password $decryptedPassword was not decrypted correctly, should be $correctPassword")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
  }

}

object RequesterActor {

  def props: Props = Props[RequesterActor]

  case class RegisterSupervisor(actorRef: ActorRef, workerNum: Int)

}
