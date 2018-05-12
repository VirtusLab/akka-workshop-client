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
  case object Sending    extends State

  sealed trait Data
  case object Uninitialized extends Data

  sealed trait Messages
  final case class Prepared(encrypted: String, prepared: PasswordPrepared) extends Messages
  final case class Decoded(prepared: Prepared, decoded: PasswordDecoded)   extends Messages
  final case class Decrypted(decoded: Decoded, decrypted: String)          extends Messages

}

import Worker._

class Worker(token: String, remoteActor: ActorRef) extends FSM[State, Data] with ActorLogging {

  startWith(Idle, Uninitialized)

  val decrypter = new Decrypter

  when(Idle) {
    case Event(Start, _) =>
      remoteActor ! SendMeEncryptedPassword(token)
      goto(Preparing)

    case Event(PasswordCorrect(decryptedPassword), _) =>
      log.debug(s"PasswordCorrect: $decryptedPassword")
      remoteActor ! SendMeEncryptedPassword(token)
      goto(Idle) using Uninitialized
    case Event(PasswordIncorrect(decryptedPassword, correctPassword), _) =>
      log.debug(s"PasswordIncorrect: $decryptedPassword should be $correctPassword")
      remoteActor ! SendMeEncryptedPassword(token)
      goto(Idle) using Uninitialized

    //In case of restart, take step back as result can be broken
    case Event(encrypted @ EncryptedPassword(encryptedPassword), _) =>
      self ! EncryptedPassword(encryptedPassword)
      goto(Preparing)
    case Event(prepared: Prepared, _) =>
      self ! EncryptedPassword(prepared.encrypted)
      goto(Preparing)
    case Event(decoded: Decoded, _) =>
      self ! decoded.prepared
      goto(Decoding)
    case Event(decrypted: Decrypted, _) =>
      self ! decrypted.decoded
      goto(Decrypting)
  }

  when(Preparing) {
    case Event(EncryptedPassword(encryptedPassword), _) =>
      self ! Prepared(encryptedPassword, decrypter.prepare(encryptedPassword))
      goto(Decoding)
  }

  when(Decoding) {
    case Event(prepared: Prepared, _) =>
      self ! Decoded(prepared, decrypter.decode(prepared.prepared))
      goto(Decrypting)
  }

  when(Decrypting) {
    case Event(decoded: Decoded, _) =>
      self ! Decrypted(decoded, decrypter.decrypt(decoded.decoded))
      goto(Sending)
  }

  when(Sending) {
    case Event(decrypted: Decrypted, _) =>
      remoteActor ! ValidateDecodedPassword(token, decrypted.decoded.prepared.encrypted, decrypted.decrypted)
      goto(Idle) using Uninitialized
  }

  whenUnhandled {
    case e => log.error(s"stateName: $stateName, unhandlerd: $e"); println(stateName)
      stay()
  }
  onTransition{
    case e -> e2 => println(s"current state: $e next state: $e2")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { m =>
      self ! m
    }

  initialize()
}
