package client

import java.util.concurrent._

import cats.effect.{ExitCode, IO, IOApp, Timer}
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

    // Notice that if you don't handle exceptions from Decrypter
    // they will propagate to this point failing the program.
    // What should we do in this case?
    decryptingLoop(client, token, decrypter)
  }

  def decryptingLoop(client: PasswordClient[IO], token: Token, decrypter: Decrypter)
                    (implicit timer: Timer[IO]): IO[Unit] = {
    // TODO: this `IO` should request, encrypt and validate passwords in infinite loop
    ???
  }

  def getPassword(client: PasswordClient[IO], token: Token): IO[Password] = {
    client.requestPassword(token)
  }
}