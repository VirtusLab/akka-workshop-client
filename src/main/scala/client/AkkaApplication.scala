package client

import akka.actor.{Props, ActorRef, Actor, ActorSystem}
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import com.virtuslab.akkaworkshop.{PasswordsDistributor, Decrypter}
import scala.util.Try


class WorkflowActor extends Actor {

  var currentToken: Token = _
  var remoteActor: ActorRef = _

  val decrypter = new Decrypter


  private def requestNewPassword(): Unit = remoteActor ! SendMeEncryptedPassword(currentToken)

  private def decryptPassword(password: String): Try[String] = Try {
    val preapared = decrypter.prepare(password)
    val decoded = decrypter.decode(preapared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }


  override def receive: Receive = {

    //start with actor from server
    case actorRef: ActorRef =>
      remoteActor = actorRef

      //register a actor
      actorRef ! Register(s"From machine: ${System.getProperty("user.name")}")

    //once actor is register ask for first password
    case Registered(token) =>
      currentToken = token
      requestNewPassword()

    //once you got a password to decrypt - do it!
    case EncryptedPassword(encryptedPassword) =>
      decryptPassword(encryptedPassword).foreach(decryptedPassword =>
        remoteActor ! ValidateDecodedPassword(currentToken, encryptedPassword, decryptedPassword))


    //correct password
    case PasswordCorrect(password) =>
      println(s"Correct password: $password")
      requestNewPassword()

    //incorrect password
    case PasswordIncorrect(password) =>
      println(s"Incorrect password: $password")

      requestNewPassword()
  }
}

/**
 * Author: Krzysztof Romanowski
 */
object AkkaApplication extends App {
  println("### Starting simple application ###")

  //actor system
  val system = ActorSystem("HelloSystem")

  //distributed actor - refactor to remote one
  val distributorActor = system.actorOf(Props[PasswordsDistributor], "sample-distributor")

  //to be replaced with solid code
  val listenerActor = system.actorOf(Props[WorkflowActor])

  listenerActor ! distributorActor

  system.awaitTermination()
}
