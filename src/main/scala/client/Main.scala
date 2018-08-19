package client

import client.PasswordClient._
import client.Decrypting._
import com.virtuslab.akkaworkshop.Decrypter
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import scalaz.zio._
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import util.putStrLn

object Main extends App {

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    Http1Client[Task]()
      .bracket(_.shutdown.attempt.void) { implicit client =>
        for {
          token <- requestToken("Łukasz")(client)
          _     <- decryptForever(token)
        } yield ()
      }
      .attempt
      .map {
        case Left(_)  => ExitStatus.ExitNow(1)
        case Right(_) => ExitStatus.ExitNow(0)
      }

  def decryptForever(token: Token)(implicit httpClient: Client[Task]): IO[Nothing, Unit] =
    (for {
      decrypter <- getDecrypter
      tasks = Seq.fill(8)(decryptionTask(decrypter, token))
      _ <- IO.parTraverse(tasks)(identity).attempt.void
    } yield ()).flatMap(_ => decryptForever(token))

  def decryptionTask(decrypter: Decrypter, token: Token)(implicit httpClient: Client[Task]): IO[Throwable, Unit] =
    (for {
      password          <- getPassword(token)
      decryptedPassword <- fullDecryption(password, decrypter)
      status            <- validatePassword(token, password.encryptedPassword, decryptedPassword)
      _                 <- putStrLn(s"Status for password: ${password.encryptedPassword}: ${status.code}")
    } yield ()).attempt.flatMap {
      case Left(err) => IO.fail(err)
      case Right(_)  => decryptionTask(decrypter, token)
    }

  def getDecrypter: IO[Nothing, Decrypter] = IO.point(new Decrypter)
}
