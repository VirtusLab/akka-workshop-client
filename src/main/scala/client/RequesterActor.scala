package client

import akka.actor._
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import akka.pattern.pipe
import scala.util.Try

class RequesterActor(remote: PasswordClient) extends Actor with ActorLogging {

  var decrypter   = new Decrypter
  val name        = "Cesar"
  implicit val ec = context.dispatcher

  private def decryptPassword(password: String): Try[String] = Try {
    val prepared  = decrypter.prepare(password)
    val decoded   = decrypter.decode(prepared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  override def preStart(): Unit =
    remote.requestToken(Register(name)).pipeTo(self)

  override def receive: Receive = starting

  def starting: Receive = {
    case Registered(token) =>
      log.info(s"Registered with token $token")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
      context.become(working(token))
  }

  def working(token: String): Receive = {
    case ep @ EncryptedPassword(encryptedPassword) =>
      val decrypted = decryptPassword(encryptedPassword)
      decrypted
        .map { decryptedPassword =>
          remote.validatePassword(ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)).pipeTo(self)
        }
        .getOrElse {
          decrypter = new Decrypter
          self ! ep
        }

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"Password $decryptedPassword was decrypted successfully")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"Password $decryptedPassword was not decrypted correctly, should be $correctPassword")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
  }

}

object RequesterActor {

  def props(remote: PasswordClient) = Props(new RequesterActor(remote))

}
