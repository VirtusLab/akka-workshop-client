package client

import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{Request, Status}
import org.http4s.circe.{jsonOf, _}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object PasswordClient {

  import TaskDsls._

  def getPassword(token: Token)(implicit httpClient: Client[Task]): Task[Password] = {
    requestPassword(token)
  }

  def requestToken(userName: String)(implicit httpClient: Client[Task]): Task[Token] = {
    val req = POST(uri("http://localhost:9000/register"), User(userName).asJson)
    httpClient.expect(req)(jsonOf[Task, Token])
  }

  def requestPassword(token: Token)(implicit httpClient: Client[Task]): Task[EncryptedPassword] = {
    val req: Task[Request[Task]] = POST(uri("http://localhost:9000/send-encrypted-password"), token.asJson)
    httpClient.expect(req)(jsonOf[Task, EncryptedPassword])
  }

  def validatePassword(token: Token, encryptedPassword: String, decryptedPassword: String)
                      (implicit httpClient: Client[Task]): Task[Status] = {
    val result = ValidatePassword(token.token, encryptedPassword, decryptedPassword)
    val req: Task[Request[Task]] = POST(uri("http://localhost:9000/validate"), result.asJson)
    httpClient.status(req)
  }
}

object TaskDsls extends Http4sClientDsl[Task] with Http4sDsl[Task]