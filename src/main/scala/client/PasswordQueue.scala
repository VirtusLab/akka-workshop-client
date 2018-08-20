package client

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

trait PasswordQueue[F[_], A] {
  def take: F[Option[A]]

  def save(element: A): F[Unit]
}

object PasswordQueue {
  def create[F[_] : Sync]: F[PasswordQueue[F, Password]] =
    Ref.of[F, List[Password]](List.empty).map { state =>
      new PasswordQueue[F, Password] {
        def take: F[Option[Password]] =
          state.modify { passwords => (passwords.drop(1), passwords.headOption) }

        def save(element: Password): F[Unit] =
          state.modify { passwords =>
            (element :: passwords, ())
          }
      }
    }
}