package client

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Status
import org.http4s.circe.{jsonOf, _}
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.{POST, uri}

object PasswordClient {

  def getPassword(token: Token)(implicit httpClient: Client[IO]): IO[Password] = {
    requestPassword(token)
  }

  def requestToken(userName: String)(implicit httpClient: Client[IO]): IO[Token] = {
    val req = POST(uri("http://localhost:9000/register"), User(userName).asJson)
    httpClient.expect(req)(jsonOf[IO, Token])
  }

  def requestPassword(token: Token)(implicit httpClient: Client[IO]): IO[EncryptedPassword] = {
    val req = POST(uri("http://localhost:9000/send-encrypted-password"), token.asJson)
    httpClient.expect(req)(jsonOf[IO, EncryptedPassword])
  }

  def validatePassword(token: Token, encryptedPassword: String, decryptedPassword: String)
                      (implicit httpClient: Client[IO]): IO[Status] = {
    val result = ValidatePassword(token.token, encryptedPassword, decryptedPassword)
    val req = POST(uri("http://localhost:9000/validate"), result.asJson)
    httpClient.status(req)
  }
}