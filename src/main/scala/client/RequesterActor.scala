package client

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, AllForOneStrategy, Props}
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import akka.pattern.pipe

object RequesterActorClusterSingleton {
  case class GetToken(nodeSupervisor: ActorRef)
  case class ReceiveToken(token: String)
}

class RequesterActorClusterSingleton(nickname: String) extends Actor with ActorLogging {
  import RequesterActorClusterSingleton._

  import context.system
  import context.dispatcher

  //register
  val passwordClient = new PasswordClient()
  passwordClient.requestToken(Register(nickname)).pipeTo(self)

  val restartingStrategy: AllForOneStrategy =
    AllForOneStrategy() { case _: Exception => Restart }

  // receive with messages that can be sent by the server
  override def receive: Receive = initializing(Set.empty[ActorRef])

  def initializing(awaitingSupervisors: Set[ActorRef]): Receive = {
    case Registered(token) =>
      //send password distributor and node to awaiting nodes
      log.info(s"Singleton $nickname has been registered")
      awaitingSupervisors.foreach(_ ! RequesterActorClusterSingleton.ReceiveToken(token))
      context.become(withToken(token))

    //store all awaiting nodes
    case GetToken(nodeSupervisor) =>
      log.info(s"New node is awaiting for password distributor ${nodeSupervisor.path}")
      context.become(initializing(awaitingSupervisors + nodeSupervisor))
  }

  def withToken(token: String): Receive = {
    case GetToken(nodeSupervisor) =>
      log.info(s"New node has joined ${nodeSupervisor.path}")
      nodeSupervisor ! RequesterActorClusterSingleton.ReceiveToken(token)
  }
}

class NodeSupervisor(requesterSingleton: ActorRef) extends Actor {

  implicit val system = context.system
  requesterSingleton ! RequesterActorClusterSingleton.GetToken(self)

  val workersNumber = 7

  override val supervisorStrategy =
    AllForOneStrategy() { case _: Exception => Restart }

  override def receive: Receive = {
    case RequesterActorClusterSingleton.ReceiveToken(token) =>
      (1 to workersNumber).foreach(
        _ => context.actorOf(Props(classOf[Worker], token, new PasswordClient())) ! Worker.Start
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

class Worker(token: String, remote: PasswordClient) extends Actor with ActorLogging {
  import client.Worker._
  import context.dispatcher

  val decrypter = new Decrypter

  log.info("New worker has been started")

  override def receive: Receive = idle()

  def idle(): Receive = {
    case Start =>
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
      context.become(preparing())

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"PasswordCorrect: $decryptedPassword")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"PasswordIncorrect: $decryptedPassword should be $correctPassword")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

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
      remote
        .validatePassword(
          ValidateDecodedPassword(
            token = token,
            encryptedPassword = decrypted.decoded.prepared.passwordEncrypted,
            decryptedPassword = decrypted.passwordDecrypted
          )
        )
        .pipeTo(self)
      context.become(idle())
  }

  //try to continue with the message from before restart
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { m =>
      self ! m
    }

}
