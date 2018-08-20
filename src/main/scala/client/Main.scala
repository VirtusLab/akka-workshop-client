package client

import java.util.concurrent._

import cats.effect.{ExitCode, IO, IOApp, Timer}
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
          _ <- decryptForever(client, token)(timer)
        } yield ExitCode.Success
      }(_.shutdown)

  def decryptForever(client: PasswordClient[IO], token: Token)(implicit timer: Timer[IO]): IO[Unit] = {
    val decrypter = new Decrypter

    decryptingLoop(client, token, decrypter)
      .handleErrorWith(_ => decryptForever(client, token))
  }

  def decryptingLoop(client: PasswordClient[IO], token: Token, decrypter: Decrypter)
                    (implicit timer: Timer[IO]): IO[Unit] = {
    for {
      password <- PasswordClient.getPassword(client, token)
      decrypted <- fullDecryption(password, decrypter)
      _ <- client.validatePassword(token, password.encryptedPassword, decrypted)
      _ <- decryptingLoop(client, token, decrypter)
    } yield ()
  }

  def getPassword[F[+_]](client: PasswordClient[F], token: Token): F[Password] = {
    client.requestPassword(token)
  }
}