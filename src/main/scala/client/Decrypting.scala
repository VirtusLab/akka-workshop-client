package client

import cats.effect.{IO, Timer}
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}

object Decrypting {

  def fullDecryption(password: Password, decrypter: Decrypter)
                    (implicit timer: Timer[IO]): IO[String] = {
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