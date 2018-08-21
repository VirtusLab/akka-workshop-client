package client

import akka.actor.Actor
import com.virtuslab.akkaworkshop.Decrypter
import com.virtuslab.akkaworkshop.PasswordsDistributor._

class RequesterActor(passwordClient: PasswordClient) extends Actor {

  val decrypter = new Decrypter

  private def decryptPassword(password: String): String = {
    val prepared  = decrypter.prepare(password)
    val decoded   = decrypter.decode(prepared)
    val decrypted = decrypter.decrypt(decoded)
    decrypted
  }

  override def preStart(): Unit = {
    // TODO Register yourself by sending Register("your nick") - passwordClient.requestToken(...)
    // HINT: Use pipeTo pattern (remember about execution context - context.dispatcher can be used)
    // https://doc.akka.io/docs/akka/2.5.5/scala/actors.html#ask-send-and-receive-future
    // send this token back to yourself - use `self` reference
  }

  // receive with messages that can be sent by the server
  override def receive: Receive = {
    case Registered(token) =>
      // TODO You are registered
      // Now we need to sore ths token and use it to request and check your passwords
      // use SendMeEncryptedPassword(token) - passwordClient.requestPassword(...)
      println(s"Registered! Token: $token")

    case EncryptedPassword(encryptedPassword) =>
      // TODO Decrypt this password using decrypter instance
      // And send it back using ValidateDecodedPassword(token, encryptedPassword, decryptedPassword)
      //password.validatePassword(...)
      println(s"Password to decrypt: $encryptedPassword")

    case PasswordCorrect(decryptedPassword) =>
      // TODO ask for more passwords
      println(s"Correctly decrypted: $decryptedPassword")

    case PasswordIncorrect(decryptedPassword, original) =>
      // TODO ask for more passwords
      println(s"Incorrectly decrypted: $decryptedPassword from $original")
  }
}
