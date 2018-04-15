package client

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, AllForOneStrategy, FSM, Props}
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import scala.concurrent.duration._

case class GetPasswordDistributor(nodeSupervisor: ActorRef)
case class ReceivePasswordDistributor(token: String, passwordDistributor: ActorRef)

class RequesterActorSingleton(nickname: String) extends Actor with ActorLogging {

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
    case remoteActorRef: ActorRef =>
      // HINT: Using actor ref instead of actor selection is recommended
      remoteActorRef ! Register(nickname)

      context.become(remoteRef(remoteActorRef, awaitingSupervisors))
    case GetPasswordDistributor(nodeSupervisor) =>
      context.become(initializing(awaitingSupervisors + nodeSupervisor))
  }

  def remoteRef(remoteActorRef: ActorRef, awaitingSupervisors: Set[ActorRef]): Receive = {
    case Registered(token) =>
      awaitingSupervisors.foreach(_ ! ReceivePasswordDistributor(token, remoteActorRef))
      context.become(withToken(remoteActorRef, token))
  }

  def withToken(remoteActorRef: ActorRef, token: String): Receive = {
    case GetPasswordDistributor(nodeSupervisor) => {
      nodeSupervisor ! ReceivePasswordDistributor(token, remoteActorRef)
    }

    case e =>
      log.error(s"This should not happen $e")
  }
}

class Supervisor(requesterSingleton: ActorRef) extends Actor {

  requesterSingleton ! GetPasswordDistributor(self)

  val workersNumber = 4

  override val supervisorStrategy =
    AllForOneStrategy() { case _: Exception => Restart }

  override def receive: Receive = {
    case ReceivePasswordDistributor(token, passwordDistributor) =>
      (1 to workersNumber).foreach(
        _ => context.actorOf(Props(classOf[Worker], token, passwordDistributor)) ! Worker.Start
      )
  }
}

object Worker {

  case object Start

  sealed trait State
  case object Idle       extends State
  case object Preparing  extends State
  case object Decoding   extends State
  case object Decrypting extends State

  sealed trait Data
  case object Uninitialized                                                        extends Data
  final case class Encrypted(encryptedPassword: String)                            extends Data
  final case class Prepared(encryptedPassword: String, prepared: PasswordPrepared) extends Data
  final case class Decoded(encryptedPassword: String, decoded: PasswordDecoded)    extends Data
  final case class Decrypted(encryptedPassword: String, decrypted: String)         extends Data
}

import Worker._

class Worker(token: String, remoteActor: ActorRef) extends FSM[State, Data] with ActorLogging {

  startWith(Idle, Uninitialized)

  val decrypter = new Decrypter

  when(Idle) {
    case Event(Start, _) =>
      remoteActor ! SendMeEncryptedPassword(token)
      stay() using Uninitialized

    case Event(EncryptedPassword(encryptedPassword), _) =>
      self ! Prepared(encryptedPassword, decrypter.prepare(encryptedPassword))
      goto(Preparing) using Encrypted(encryptedPassword)


    case Event(PasswordCorrect(decryptedPassword), _) =>
      log.debug(s"PasswordCorrect: $decryptedPassword")
      remoteActor ! SendMeEncryptedPassword(token)
      stay() using Uninitialized
    case Event(PasswordIncorrect(decryptedPassword, correctPassword), _) =>
      log.debug(s"PasswordIncorrect: $decryptedPassword should be $correctPassword")
      remoteActor ! SendMeEncryptedPassword(token)
      stay() using Uninitialized

    //In case of restart
    case Event(Prepared(encryptedPassword, prepared), _) =>
      self ! Decoded(encryptedPassword, decrypter.decode(prepared))
      goto(Decoding) using Prepared(encryptedPassword, prepared)
    case Event(Decoded(encryptedPassword, decoded), _) =>
      self ! Decrypted(encryptedPassword, decrypter.decrypt(decoded))
      goto(Decrypting) using Decoded(encryptedPassword, decoded)
    case Event(Decrypted(encryptedPassword, decrypted), _) =>
      remoteActor ! ValidateDecodedPassword(token, encryptedPassword, decrypted)
      goto(Idle) using Decrypted(encryptedPassword, decrypted)
  }

  when(Preparing) {
    case Event(Prepared(encryptedPassword, prepared), _) =>
      self ! Decoded(encryptedPassword, decrypter.decode(prepared))
      goto(Decoding) using Prepared(encryptedPassword, prepared)
  }

  when(Decoding) {
    case Event(Decoded(encryptedPassword, decoded), _) =>
      self ! Decrypted(encryptedPassword, decrypter.decrypt(decoded))
      goto(Decrypting) using Decoded(encryptedPassword, decoded)
  }

  when(Decrypting) {
    case Event(Decrypted(encryptedPassword, decrypted), _) =>
      remoteActor ! ValidateDecodedPassword(token, encryptedPassword, decrypted)
      goto(Idle) using Decrypted(encryptedPassword, decrypted)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { m =>
      self ! m
    }

  initialize()
}
