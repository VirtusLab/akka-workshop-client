package client

import client.PasswordClient._
import client.Decrypting._
import com.virtuslab.akkaworkshop.Decrypter
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import scalaz.zio._
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object Main extends App {

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    Http1Client[Task]()
      .bracket(_.shutdown.attempt.void) { implicit client =>
        for {
          token <- requestToken("Łukasz")(client)
          queue <- Ref(List.empty[Password])
          _     <- decryptForever(token, queue)
        } yield ()
      }
      .attempt
      .map {
        case Left(_)  => ExitStatus.ExitNow(1)
        case Right(_) => ExitStatus.ExitNow(0)
      }

  def decryptForever(token: Token, queue: Ref[List[Password]])(implicit httpClient: Client[Task]): IO[Nothing, Unit] =
    (for {
      decrypter   <- getDecrypter
      failureFlag <- Ref[Boolean](false)

      tasks = Seq.fill(8)(decryptionTask(decrypter, token, queue, failureFlag))

      _ <- IO.parTraverse(tasks)(identity).attempt.void
    } yield ()).flatMap(_ => decryptForever(token, queue))

  def decryptionTask(decrypter: Decrypter, token: Token, queue: Ref[List[Password]], failureFlag: Ref[Boolean])(
    implicit httpClient: Client[Task]
  ): IO[Nothing, Unit] =
    (for {
      password          <- passwordFromQueueOrNew(queue, token)
      decryptedPassword <- fullDecryption(password, decrypter, queue, failureFlag)
      _                 <- validatePassword(token, password.encryptedPassword, decryptedPassword)
    } yield ()).attempt.flatMap {
      case Left(_)  => IO.unit
      case Right(_) => decryptionTask(decrypter, token, queue, failureFlag)
    }

  def passwordFromQueueOrNew(queueRef: Ref[List[Password]], token: Token)(
    implicit httpClient: Client[Task]
  ): IO[Throwable, Password] =
    queueRef.modify(queue => (queue.headOption, queue.drop(1))).flatMap {
      case Some(failedPassword) => IO.point(failedPassword)
      case None                 => getPassword(token)
    }

  def getDecrypter: IO[Nothing, Decrypter] = IO.point(new Decrypter)
}
