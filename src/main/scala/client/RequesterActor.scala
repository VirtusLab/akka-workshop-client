package client

import scala.concurrent.duration._

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, AllForOneStrategy, Props}
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}

object RequesterActorClusterSingleton {
  case class GetPasswordDistributor(nodeSupervisor: ActorRef)
  case class ReceivePasswordDistributor(token: String, passwordDistributor: ActorRef)
}

class RequesterActorClusterSingleton(nickname: String) extends Actor with ActorLogging {
  import RequesterActorClusterSingleton._

  import context.dispatcher

  val restartingStrategy: AllForOneStrategy =
    AllForOneStrategy() { case _: Exception => Restart }

  context
    .actorSelection(
      "akka.tcp://application@headquarters:9552/user/PasswordsDistributor"
    )
    .resolveOne(5.seconds)
    .foreach(self ! _)

  // receive with messages that can be sent by the server
  override def receive: Receive = initializing(Set.empty[ActorRef])

  def initializing(awaitingSupervisors: Set[ActorRef]): Receive = {
    case passwordDistributor: ActorRef =>
      passwordDistributor ! Register(nickname)
      context.become(remoteRef(passwordDistributor, awaitingSupervisors))

    //store all awaiting nodes
    case GetPasswordDistributor(nodeSupervisor) =>
      log.info(s"New node is awaiting for password distributor ${nodeSupervisor.path}")
      context.become(initializing(awaitingSupervisors + nodeSupervisor))
  }

  def remoteRef(passwordDistributor: ActorRef, awaitingSupervisors: Set[ActorRef]): Receive = {
    case Registered(token) =>
      //send password distributor and node to awaiting nodes
      log.info(s"Singleton $nickname has been registered")
      awaitingSupervisors.foreach(_ ! ReceivePasswordDistributor(token, passwordDistributor))
      context.become(withToken(passwordDistributor, token))
  }

  def withToken(passwordDistributor: ActorRef, token: String): Receive = {
    case GetPasswordDistributor(nodeSupervisor) =>
      log.info(s"New node has joined ${nodeSupervisor.path}")
      nodeSupervisor ! ReceivePasswordDistributor(token, passwordDistributor)
  }
}

class NodeSupervisor(requesterSingleton: ActorRef) extends Actor {

  requesterSingleton ! RequesterActorClusterSingleton.GetPasswordDistributor(self)

  val workersNumber = 4

  override val supervisorStrategy =
    AllForOneStrategy() { case _: Exception => Restart }

  override def receive: Receive = {
    case RequesterActorClusterSingleton.ReceivePasswordDistributor(token, passwordDistributor) =>
      (1 to workersNumber).foreach(
        _ => context.actorOf(Props(classOf[Worker], token, passwordDistributor)) ! Worker.Start
      )
  }
}

object Worker {

  case object Start

  sealed trait Messages
  final case class Prepared(passwordEncrypted: String, passwordPrepared: PasswordPrepared) extends Messages
  final case class Decoded(prepared: Prepared, passwordDecoded: PasswordDecoded)           extends Messages
  final case class Decrypted(decoded: Decoded, passwordDecrypted: String)                  extends Messages
}

class Worker(token: String, remoteActor: ActorRef) extends Actor with ActorLogging {
  import client.Worker._

  val decrypter = new Decrypter

  log.info("New worker has been started")

  override def receive: Receive = idle()

  def idle(): Receive = {
    case Start =>
      remoteActor ! SendMeEncryptedPassword(token)
      context.become(preparing())

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"PasswordCorrect: $decryptedPassword")
      remoteActor ! SendMeEncryptedPassword(token)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"PasswordIncorrect: $decryptedPassword should be $correctPassword")
      remoteActor ! SendMeEncryptedPassword(token)

    //In case of restart, take step back as result can be broken
    case encrypted: EncryptedPassword =>
      self ! encrypted
      context.become(preparing())

    case prepared: Prepared =>
      self ! EncryptedPassword(prepared.passwordEncrypted)
      context.become(preparing())

    case decoded: Decoded =>
      self ! decoded.prepared
      context.become(decoding())

    case decrypted: Decrypted =>
      self ! decrypted.decoded
      context.become(decrypting())
  }

  def preparing(): Receive = {
    case EncryptedPassword(encryptedPassword) =>
      self ! Prepared(encryptedPassword, decrypter.prepare(encryptedPassword))
      context.become(decoding())
  }

  def decoding(): Receive = {
    case prepared: Prepared =>
      self ! Decoded(prepared, decrypter.decode(prepared.passwordPrepared))
      context.become(decrypting())
  }

  def decrypting(): Receive = {
    case decoded: Decoded =>
      self ! Decrypted(decoded, decrypter.decrypt(decoded.passwordDecoded))
      context.become(sending())
  }

  def sending(): Receive = {
    case decrypted: Decrypted =>
      remoteActor ! ValidateDecodedPassword(
        token = token,
        encryptedPassword = decrypted.decoded.prepared.passwordEncrypted,
        decryptedPassword = decrypted.passwordDecrypted
      )
      context.become(idle())
  }

  //try to continue with the message from before restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { m =>
      self ! m
    }

}
