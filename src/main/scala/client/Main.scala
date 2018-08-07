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
      .flatMap { client =>
        implicit val httpClient: Client[IO] = client
        for {
          token <- requestToken("Piotrek")
          _ <- decryptForever(token)
          _ <- httpClient.shutdown
        } yield ExitCode.Success
      }

  def decryptForever(token: Token)(implicit httpClient: Client[IO]): IO[Unit] = {
    val decrypter = new Decrypter

    // Notice that if you don't handle exceptions from Decrypter
    // they will propagate to this point failing the program.
    // What should we do in this case?
    decryptingLoop(token, decrypter)
  }

  def decryptingLoop(token: Token, decrypter: Decrypter)
                    (implicit httpClient: Client[IO]): IO[Unit] = {
    // TODO: this `IO` should request, encrypt and validate passwords in infinite loop
    ???
  }
}