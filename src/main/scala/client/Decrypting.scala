package client

import cats.effect.concurrent.Ref
import cats.effect.{IO, Timer}
import cats.syntax.applicativeError._
import cats.syntax.apply._
import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}

object Decrypting {

  def fullDecryption(password: Password, decrypter: Decrypter, cancelSignal: Ref[IO, Boolean],
                     passwordQueue: PasswordQueue[IO, Password])(implicit timer: Timer[IO]): IO[String] = {
    def handleError: PartialFunction[Throwable, IO[Unit]] = {
      case _ => cancelSignal.set(true) *> passwordQueue.save(password)
    }

    def checkCancel(): IO[Unit] =
      for {
        shouldStop <- cancelSignal.get
        result <- if (shouldStop) passwordQueue.save(password) *> IO.raiseError(CancelException) else IO.unit
      } yield result

    password match {
      case EncryptedPassword(encrypted) =>
        for {
          _ <- checkCancel()
          prepared <- preparePassword(encrypted, decrypter).onError(handleError)
          _ <- checkCancel()
          decrypted <- fullDecryption(PreparedPassword(password.encryptedPassword, prepared), decrypter, cancelSignal, passwordQueue)
        } yield decrypted

      case PreparedPassword(_, prepared) =>
        for {
          decoded <- decodePassword(prepared, decrypter).onError(handleError)
          _ <- checkCancel()
          decrypted <- fullDecryption(DecodedPassword(password.encryptedPassword, decoded), decrypter, cancelSignal, passwordQueue)
        } yield decrypted

      case DecodedPassword(_, decoded) =>
        for {
          decrypted <- decryptPassword(decoded, decrypter).onError(handleError)
          _ <- checkCancel()
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