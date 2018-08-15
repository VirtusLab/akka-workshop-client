package client

import client.PasswordClient._
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
          _     <- decryptForever(token)
        } yield ()
      }
      .attempt
      .map {
        case Left(_)  => ExitStatus.ExitNow(1)
        case Right(_) => ExitStatus.ExitNow(0)
      }

  def decryptForever(token: Token)(implicit httpClient: Client[Task]): IO[Nothing, Unit] = {
    val decrypter = new Decrypter

    // Notice that if you don't handle exceptions from Decrypter
    // they will propagate to this point failing the program.
    // What should we do in this case?
    decryptionTask(token, decrypter).forever
  }

  def decryptionTask(token: Token, decrypter: Decrypter)(implicit httpClient: Client[Task]): IO[Nothing, Unit] =
    // TODO: this `IO` should request, decrypt and validate a password
    ???
}
