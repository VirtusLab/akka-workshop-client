package client

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

trait PasswordQueue[F[_]] {
  def take: F[Option[Password]]

  def put(password: Password): F[Unit]
}

object PasswordQueue {
  def create[F[_] : Sync]: F[PasswordQueue[F]] =
    Ref.of[F, List[Password]](List.empty).map { state =>
      new PasswordQueue[F] {
        def take: F[Option[Password]] =
          state.modify { passwords => (passwords.drop(1), passwords.headOption) }

        def put(password: Password): F[Unit] =
          state.modify { passwords =>
            (password :: passwords, ())
          }
      }
    }
}