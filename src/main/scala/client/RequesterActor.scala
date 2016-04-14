package client

import akka.actor._
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._

import scala.util.Try

class RequesterActor(remote : ActorRef) extends Actor with ActorLogging{

  var decrypter = new Decrypter
  val name = "Cesar"

  private def decryptPassword(password: String): Try[String] = Try {
    val prepared = decrypter.prepare(password)
    val decoded = decrypter.decode(prepared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  override def preStart() = {
    remote ! Register(name)
  }

  override def receive: Receive = starting

  def starting: Receive = {
    case Registered(token) =>
      log.info(s"Registered with token $token")
      remote ! SendMeEncryptedPassword(token)
      context.become(working(token))
  }

  def working(token : String): Receive = {
    case ep@EncryptedPassword(encryptedPassword) =>
      val decrypted = decryptPassword(encryptedPassword)
      decrypted.map {
        decryptedPassword =>
          remote ! ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)
      }.getOrElse{
        decrypter = new Decrypter
        self ! ep
      }

    case PasswordCorrect(decryptedPassword) =>
      log.info(s"Password $decryptedPassword was decrypted successfully")
      remote ! SendMeEncryptedPassword(token)

    case PasswordIncorrect(decryptedPassword, correctPassword) =>
      log.error(s"Password $decryptedPassword was not decrypted correctly, should be $correctPassword")
      remote ! SendMeEncryptedPassword(token)
  }

}

object RequesterActor {

  def props(remote: ActorRef) = Props(classOf[RequesterActor], remote)

}