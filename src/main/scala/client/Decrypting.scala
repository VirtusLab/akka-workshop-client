package client

import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import scalaz.zio.IO
import util.putStrLn

object Decrypting {

  def fullDecryption(password: Password, decrypter: Decrypter): IO[Throwable, String] = {
    password match {
      case EncryptedPassword(encrypted) =>
        putStrLn(s"Preparing password: $encrypted") *>
          preparePassword(encrypted, decrypter).flatMap { prepared =>
            fullDecryption(PreparedPassword(encrypted, prepared), decrypter)
          }

      case PreparedPassword(encrypted, prepared) =>
        putStrLn(s"Decoding password: $encrypted") *>
          decodePassword(prepared, decrypter).flatMap { decoded =>
            fullDecryption(DecodedPassword(encrypted, decoded), decrypter)
          }

      case DecodedPassword(encrypted, decoded) =>
        putStrLn(s"Decrypting password: $encrypted") *>
          decryptPassword(decoded, decrypter)
    }
  }

  private def preparePassword(password: String, decrypter: Decrypter): IO[Throwable, PasswordPrepared] =
    IO syncThrowable {
      decrypter prepare password
    }

  private def decodePassword(passwordPrepared: PasswordPrepared, decrypter: Decrypter): IO[Throwable, PasswordDecoded] =
    IO syncThrowable {
      decrypter decode passwordPrepared
    }

  private def decryptPassword(passwordDecoded: PasswordDecoded, decrypter: Decrypter): IO[Throwable, String] =
    IO syncThrowable {
      decrypter decrypt passwordDecoded
    }
}
