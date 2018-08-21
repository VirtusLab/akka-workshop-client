package client
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import com.virtuslab.akkaworkshop.PasswordsDistributor._
import spray.json._

class PasswordClient()(implicit actorSystem: ActorSystem) extends JsonSupport {
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  private val http = Http()

  def requestToken(register: Register): Future[Registered] = {
    http
      .singleRequest(
        HttpRequest(
          HttpMethods.POST,
          "http://localhost:9000/register",
          entity = HttpEntity(ContentTypes.`application/json`, register.toJson.toString())
        )
      )
      .flatMap(r => Unmarshaller.stringUnmarshaller(r.entity).map(_.asJson.convertTo[Registered]))
  }

  def requestPassword(sendMeEncryptedPassword: SendMeEncryptedPassword): Future[EncryptedPassword] = {
    http
      .singleRequest(
        HttpRequest(
          HttpMethods.POST,
          "http://localhost:9000/send-encrypted-password",
          entity = HttpEntity(ContentTypes.`application/json`, sendMeEncryptedPassword.toJson.toString())
        )
      )
      .flatMap(r => Unmarshaller.stringUnmarshaller(r.entity).map(_.asJson.convertTo[EncryptedPassword]))
  }

  def validatePassword(validatePassword: ValidateDecodedPassword): Future[PDMessageResponse] = {
    http
      .singleRequest(
        HttpRequest(
          HttpMethods.POST,
          "http://localhost:9000/validate",
          entity = HttpEntity(ContentTypes.`application/json`, validatePassword.toJson.toString())
        )
      )
      .flatMap {
        case r if r.status == StatusCodes.OK =>
          Unmarshaller.stringUnmarshaller(r.entity).map(_.asJson.convertTo[PasswordCorrect])
        case r => Unmarshaller.stringUnmarshaller(r.entity).map(_.asJson.convertTo[PasswordIncorrect])
      }
  }
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val registerFormat: JsonFormat[Register]                               = jsonFormat1(Register)
  implicit val registeredFormat: JsonFormat[Registered]                           = jsonFormat1(Registered)
  implicit val sendMeEncryptedPasswordFormat: JsonFormat[SendMeEncryptedPassword] = jsonFormat1(SendMeEncryptedPassword)
  implicit val encryptedPasswordFormat: JsonFormat[EncryptedPassword]             = jsonFormat1(EncryptedPassword)
  implicit val passwordCorrectFormat: JsonFormat[PasswordCorrect]                 = jsonFormat1(PasswordCorrect)
  implicit val passwordIncorrectFormat: JsonFormat[PasswordIncorrect]             = jsonFormat2(PasswordIncorrect)
  implicit val validatePasswordFormat: JsonFormat[ValidateDecodedPassword]        = jsonFormat3(ValidateDecodedPassword)
}
