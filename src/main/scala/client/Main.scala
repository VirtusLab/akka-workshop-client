package client

import java.util.concurrent._

import cats.effect.{ExitCode, IO, IOApp, Timer}
import com.virtuslab.akkaworkshop.Decrypter
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import client.Decrypting._
import client.PasswordClient._
import cats.instances.list._
import cats.syntax.all._

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def timer: Timer[IO] = IO.timer(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

  override def run(args: List[String]): IO[ExitCode] =
    Http1Client[IO]()
      .bracket { client =>
        for {
          token <- requestToken("Piotrek")(client)
          _ <- decryptForever(2, token)(client, timer)
        } yield ExitCode.Success
      }(_.shutdown)

  def decryptForever(parallelism: Int, token: Token)(implicit httpClient: Client[IO], timer: Timer[IO]): IO[Unit] = {
    val decrypter = new Decrypter
    List
      .fill(parallelism)(decryptingLoop(token, decrypter).handleErrorWith(_ => IO.unit))
      .parSequence
      .flatMap(_ => decryptForever(parallelism, token))
  }

  def decryptingLoop(token: Token, decrypter: Decrypter)
                    (implicit httpClient: Client[IO], timer: Timer[IO]): IO[Unit] = {
    for {
      password <- getPassword(token)
      decrypted <- fullDecryption(password, decrypter)
      _ <- validatePassword(token, password.encryptedPassword, decrypted)
      _ <- decryptingLoop(token, decrypter)
    } yield ()
  }
}