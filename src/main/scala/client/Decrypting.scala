package client

import cats.effect.{IO, Timer}
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}

object Decrypting {

  def fullDecryption(password: Password, decrypter: Decrypter)
                    (implicit timer: Timer[IO]): IO[String] = {
    password match {
      case EncryptedPassword(encrypted) =>
        for {
          prepared <- preparePassword(encrypted, decrypter)
          decrypted <- fullDecryption(PreparedPassword(password.encryptedPassword, prepared), decrypter)
        } yield decrypted

      case PreparedPassword(_, prepared) =>
        for {
          decoded <- decodePassword(prepared, decrypter)
          decrypted <- fullDecryption(DecodedPassword(password.encryptedPassword, decoded), decrypter)
        } yield decrypted

      case DecodedPassword(_, decoded) =>
        for {
          decrypted <- decryptPassword(decoded, decrypter)
        } yield decrypted
    }
  }

  private def preparePassword(password: String, decrypter: Decrypter): IO[PasswordPrepared] =
    IO(decrypter.prepare(password))

  private def decodePassword(passwordPrepared: PasswordPrepared, decrypter: Decrypter): IO[PasswordDecoded] =
    IO(decrypter.decode(passwordPrepared))

  private def decryptPassword(passwordDecoded: PasswordDecoded, decrypter: Decrypter): IO[String] =
    IO(decrypter.decrypt(passwordDecoded))
}