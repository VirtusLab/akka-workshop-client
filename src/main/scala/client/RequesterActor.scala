package client

import akka.actor.{ActorSelection, Props, Actor}
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._

class RequesterActor extends Actor {

  val decrypter = new Decrypter

  private def decryptPassword(password: String): String = {
    val preapared = decrypter.prepare(password)
    val decoded = decrypter.decode(preapared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  // receive with messages that can be sent by the server
  override def receive: Receive = {
    case remoteActorSelection: ActorSelection =>
      // TODO Register yourself by sending Register("your nick") to remote actor from selection
      // HINT: Using actor ref instead of actor selection is recommended
      println(s"Got remote actor selection: $remoteActorSelection")

    case Registered(token) =>
      // TODO You are registered
      // Now we need to sore ths token and use it to request and check your passwords
      // use SendMeEncryptedPassword(token)
      println(s"Registered! Token: $token")

    case EncryptedPassword(encryptedPassword) =>
      // TODO Decrypt this password using decrypter instance
      // And send it back using ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)
      println(s"Password to decrypt: $encryptedPassword")

    case PasswordCorrect(decryptedPassword) =>
      // TODO ask for more passwords
      println(s"Correctly decrypted: $decryptedPassword")

    case PasswordIncorrect(decryptedPassword, original) =>
      // TODO ask for more passwords
      println(s"Incorrectly decrypted: $decryptedPassword from $original")
  }
}
