package client

import java.util.concurrent._

import cats.effect.concurrent.Ref
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
          cancelSignal <- Ref.of[IO, Boolean](false)
          passwordQueue <- Ref[IO].of(List[Password]())
          _ <- decryptForever(12, token, cancelSignal, passwordQueue)(client, timer)
        } yield ExitCode.Success
      }(_.shutdown)

  def decryptForever(parallelism: Int, token: Token, cancelSignal: Ref[IO, Boolean], passwordQueue: Ref[IO, List[Password]])
                    (implicit httpClient: Client[IO], timer: Timer[IO]): IO[Unit] = {
    val decrypter = new Decrypter
    List
      .fill(parallelism)(decryptingLoop(token, decrypter, cancelSignal, passwordQueue).handleErrorWith(_ => IO.unit))
      .parSequence
      .flatMap(_ => Ref.of[IO, Boolean](false).flatMap(decryptForever(parallelism, token, _, passwordQueue)))
  }

  def decryptingLoop(token: Token, decrypter: Decrypter, cancelSignal: Ref[IO, Boolean], passwordQueue: Ref[IO, List[Password]])
                    (implicit httpClient: Client[IO], timer: Timer[IO]): IO[Unit] = {
    for {
      password <- getPassword(token, passwordQueue)
      decrypted <- fullDecryption(password, decrypter, cancelSignal, passwordQueue: Ref[IO, List[Password]])
      _ <- validatePassword(token, password.encryptedPassword, decrypted)
      _ <- decryptingLoop(token, decrypter, cancelSignal, passwordQueue)
    } yield ()
  }
}