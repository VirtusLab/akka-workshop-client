package client

import akka.actor.{Actor, Props}
import com.virtuslab.akkaworkshop.PasswordsDistributor.ValidateDecodedPassword
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}

class Worker extends Actor {

  val decrypter = new Decrypter

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message foreach { self.forward }

  override def receive: Actor.Receive = waitingForNewPassword

  def waitingForNewPassword: Actor.Receive = {
    case (encryptedPassword: String) :: Nil =>
      self forward (decrypter.prepare(encryptedPassword) :: List(encryptedPassword))
      context.become(processing)
    case _ :: history => //take step back - discard last computation result (after restart it can be invalid)
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

object Worker {
  def props: Props = Props[Worker]
}
