package client

import java.util.concurrent._

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.instances.list._
import cats.syntax.all._
import client.Decrypting.fullDecryption
import com.virtuslab.akkaworkshop.Decrypter
import org.http4s.client.blaze.Http1Client

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def timer: Timer[IO] = IO.timer(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

  override def run(args: List[String]): IO[ExitCode] =
    Http1Client[IO]()
      .bracket { httpClient =>
        val client = PasswordClient.create(httpClient)
        for {
          token <- client.requestToken("Piotrek")
          cancelSignal <- Ref.of[IO, Boolean](false)
          passwordQueue <- PasswordQueue.create[IO]
          _ <- decryptForever(12, client, token, cancelSignal, passwordQueue)(timer)
        } yield ExitCode.Success
      }(_.shutdown)

  def decryptForever(parallelism: Int, client: PasswordClient[IO],
                     token: Token, cancelSignal: Ref[IO, Boolean],
                     passwordQueue: PasswordQueue[IO])(implicit timer: Timer[IO]): IO[Unit] = {
    val decrypter = new Decrypter
    List
      .fill(parallelism)(decryptingLoop(client, token, decrypter, cancelSignal, passwordQueue).handleErrorWith(_ => IO.unit))
      .parSequence
      .flatMap(_ => Ref.of[IO, Boolean](false).flatMap(decryptForever(parallelism, client, token, _)))
  }

  def decryptingLoop(client: PasswordClient[IO], token: Token,
                     decrypter: Decrypter, cancelSignal: Ref[IO, Boolean],
                     passwordQueue: PasswordQueue[IO])(implicit timer: Timer[IO]): IO[Unit] = {
    for {
      password <- getPassword(client, passwordQueue, token)
      decrypted <- fullDecryption(password, decrypter, cancelSignal, passwordQueue)
      _ <- client.validatePassword(token, password.encryptedPassword, decrypted)
      _ <- decryptingLoop(client, token, decrypter, cancelSignal, passwordQueue)
    } yield ()
  }

  def getPassword(client: PasswordClient[IO], passwordQueue: PasswordQueue[IO], token: Token): IO[Password] = {
    passwordQueue
      .take
      .flatMap {
        case Some(pass) => IO.pure(pass)
        case _ => client.requestPassword(token)
      }
  }
}