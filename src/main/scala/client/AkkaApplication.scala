package client

import akka.actor.{Actor, ActorSystem, Props}
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import scala.util.Try

class RequesterActor extends Actor {

  val decrypter = new Decrypter

  private def decryptPassword(password: String): Try[String] = Try {
    val preapared = decrypter.prepare(password)
    val decoded = decrypter.decode(preapared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  // receive with messages that can be sent by the server
  override def receive: Receive = {

    case Registered(token) => ???

    case EncryptedPassword(encryptedPassword) => ???

    case PasswordCorrect(decryptedPassword) => ???

    case PasswordIncorrect(decryptedPassword) => ???
  }

}

object RequesterActor {

  def props = Props[RequesterActor]

  // messages needed to communicate with the server
  def registerMessage(name : String) = Register(name)

  def validatePasswordMessage(token: Token,
                              encryptedPassword : String,
                              decryptedPassword : String) =
    ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)

  def sendPasswordMessage(token: Token) = SendMeEncryptedPassword(token)


}

object AkkaApplication extends App {

  val system = ActorSystem("RequesterSystem")

  val requesterActor = system.actorOf(RequesterActor.props, name = "requester")

}
