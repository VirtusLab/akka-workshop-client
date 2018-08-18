import com.virtuslab.akkaworkshop.{PasswordDecoded, PasswordPrepared}

package object client {

  case class User(name: String) extends AnyVal

  case class Token(token: String) extends AnyVal

  sealed trait Password {
    val encryptedPassword: String
  }

  case class EncryptedPassword(encryptedPassword: String) extends Password

  case class PreparedPassword(encryptedPassword: String, password: PasswordPrepared) extends Password

  case class DecodedPassword(encryptedPassword: String, password: PasswordDecoded) extends Password

  case class ValidatePassword(token: String,
                              encryptedPassword: String,
                              decryptedPassword: String)

  case object CancelException extends Throwable
}