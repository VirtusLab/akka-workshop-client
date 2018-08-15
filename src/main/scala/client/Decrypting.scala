package client

import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import scalaz.zio.IO

object Decrypting {

  def fullDecryption(password: Password, decrypter: Decrypter): IO[Nothing, String] = {
    password match {
      case EncryptedPassword(encrypted) =>
        ??? // TODO: preparePassword and continue

      case PreparedPassword(_, prepared) =>
        ??? // TODO: decodePassword and continue

      case DecodedPassword(_, decoded) =>
        ??? // TODO: decryptPassword and return
    }
  }

  private def preparePassword(password: String, decrypter: Decrypter) =
    ??? // TODO: Note that decrypter.prepare is side-effecting, blocking function

  private def decodePassword(passwordPrepared: PasswordPrepared, decrypter: Decrypter) =
    ??? // TODO: Note that decrypter.decode is side-effecting, blocking function

  private def decryptPassword(passwordDecoded: PasswordDecoded, decrypter: Decrypter) =
    ??? // TODO: Note that decrypter.decrypt is side-effecting, blocking function
}