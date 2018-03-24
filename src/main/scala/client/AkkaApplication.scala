package client

import java.net.InetAddress
import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing._
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import scala.concurrent.duration._
import scala.concurrent.Await

class WorkerActor extends Actor {
  val decrypter = new Decrypter

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { self.forward }

  override def receive: Actor.Receive = waitingForNewPassword

  def waitingForNewPassword: Actor.Receive = {
    case (encryptedPassword: String) :: Nil =>
      self forward (decrypter.prepare(encryptedPassword) :: List(encryptedPassword))
      context.become(processing)
    case msg :: history =>
      self forward history
      if (history.length > 1) context.become(processing)
  }

  def processing: Actor.Receive = {
    case (preparedPassword: PasswordPrepared) :: hs =>
      self forward (decrypter.decode(preparedPassword) :: preparedPassword :: hs)
    case (decodedPassword: PasswordDecoded) :: hs =>
      self forward (decrypter.decrypt(decodedPassword) :: decodedPassword :: hs)
    case (decryptedPassword: String) :: decodedPassword :: preparedPassword :: (encryptedPassword: String) :: Nil =>
      sender ! ValidateDecodedPassword("", encryptedPassword, decryptedPassword)
      context.become(waitingForNewPassword)
  }
}

class WorkDispatcherActor extends Actor {
  val poolSize           = 10
  var token: String      = _
  val restartingStrategy = AllForOneStrategy() { case _: Exception => Restart }
  val router = context.actorOf(
    RoundRobinPool(poolSize, supervisorStrategy = restartingStrategy)
      .props(Props[WorkerActor])
  )
  val distributorActor = context.actorSelection("akka.tcp://application@headquarters:9552/user/PasswordsDistributor")

  router ! GetRoutees

  def receive = {
    case Routees(routees) =>
      routees.foreach { case ActorRefRoutee(ref) => context.watch(ref) }
      distributorActor ! Register(InetAddress.getLocalHost.getHostName)
    case Registered(t) =>
      token = t
      (1 to poolSize).foreach(_ => requestNewPassword())
    case EncryptedPassword(password) =>
      router ! List(password)
    case vdp: ValidateDecodedPassword =>
      distributorActor ! vdp.copy(token = token)
    case PasswordCorrect(password) =>
      println(s"Correct password: $password")
      requestNewPassword()
    case PasswordIncorrect(password, correct) =>
      println(s"Incorrect password: $password; should be $correct")
      requestNewPassword()
  }

  def requestNewPassword() = distributorActor ! SendMeEncryptedPassword(token)
}

object AkkaApplication extends App {
  println("### Starting simple application ###")
  val system           = ActorSystem("WorkshopClientAkkaSystem")
  val distributorActor = system.actorSelection("akka.tcp://application@headquarters:9552/user/PasswordsDistributor")
  val dispatcher       = system.actorOf(Props[WorkDispatcherActor], "dispatcher")
  Await.ready(system.whenTerminated, Duration.Inf)
}
