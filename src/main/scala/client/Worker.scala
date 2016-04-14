package client

import akka.actor.{Actor, Props}
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor.{ValidateDecodedPassword, EncryptedPassword}

class Worker extends Actor {

  val decrypter = new Decrypter

  private def decryptPassword(password: String): String = {
    val prepared = decrypter.prepare(password)
    val decoded = decrypter.decode(prepared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  override def receive: Receive = working

  override def preRestart(reason: Throwable, message: Option[Any]) {
    message foreach { self.forward }
  }

  def working : Receive = {
    case ep@EncryptedPassword(encryptedPassword) =>
      val decrypted = decryptPassword(encryptedPassword)
      sender ! ValidateDecodedPassword("", encryptedPassword, decrypted)

  }
}

object Worker {
  def props = Props[Worker]
}