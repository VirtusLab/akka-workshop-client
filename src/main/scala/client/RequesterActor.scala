package client

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import akka.pattern.pipe

class RequesterActor(remote: PasswordClient) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  var decrypter = new Decrypter
  val name      = "Cesar"

  val workersNumber      = 5
  val restartingStrategy = AllForOneStrategy() { case _: Exception => Restart }
  val workers = context.actorOf(
    RoundRobinPool(workersNumber, supervisorStrategy = restartingStrategy)
      .props(Worker.props)
  )

  override def preStart() =
    remote.requestToken(name).pipeTo(self)

  override def receive: Receive = starting

  def starting: Receive = {
    case Registered(token) =>
      log.info(s"Registered with token $token")
      remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
      for (_ <- 0 until workersNumber) remote.requestPassword(SendMeEncryptedPassword(token)).pipeTo(self)
      context.become(working(token))
  }

  def working(token: String): Receive = {
    case encryptedPassword: EncryptedPassword =>
      workers ! encryptedPassword

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

  def props(remote: PasswordClient) = Props(new RequesterActor(remote))

}
