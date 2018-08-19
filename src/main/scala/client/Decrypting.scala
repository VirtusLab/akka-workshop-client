package client

import com.virtuslab.akkaworkshop.{Decrypter, PasswordDecoded, PasswordPrepared}
import scalaz.zio.{IO, Ref}

object Decrypting {

  import scala.concurrent.duration._

  case object ParallelFailure extends IllegalStateException

  private def checkParallelFailure(implicit p: Password, q: Ref[List[Password]], f: Ref[Boolean]): IO[Throwable, Unit] =
    for {
      _      <- IO.sleep(2.millis)
      failed <- f.get
      _      <- if (failed) q.update(p :: _) *> IO.fail(ParallelFailure) else IO.unit
    } yield ()

  private def reenqueuePassword(implicit p: Password, q: Ref[List[Password]], f: Ref[Boolean]) =
    (_: Option[Throwable]) => f.set(true) *> q.update(p :: _).void

  def fullDecryption(
    implicit
    password: Password,
    decrypter: Decrypter,
    queueRef: Ref[List[Password]],
    failureFlagRef: Ref[Boolean]
  ): IO[Throwable, String] = {
    password match {
      case EncryptedPassword(encrypted) =>
        (for {
          prepared <- preparePassword(encrypted, decrypter).onError(reenqueuePassword)
          _        <- checkParallelFailure
        } yield prepared).flatMap { prepared =>
          fullDecryption(PreparedPassword(encrypted, prepared), decrypter, queueRef, failureFlagRef)
        }
      case PreparedPassword(encrypted, prepared) =>
        (for {
          decoded <- decodePassword(prepared, decrypter).onError(reenqueuePassword)
          _       <- checkParallelFailure
        } yield decoded).flatMap { decoded =>
          fullDecryption(DecodedPassword(encrypted, decoded), decrypter, queueRef, failureFlagRef)
        }

      case DecodedPassword(_, decoded) =>
        for {
          decrypted <- decryptPassword(decoded, decrypter).onError(reenqueuePassword)
          _         <- checkParallelFailure
        } yield decrypted
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
