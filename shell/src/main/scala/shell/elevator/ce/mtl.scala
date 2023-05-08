package shell.elevator.ce

import cats.effect.Ref
import cats.effect.std.Console

import cats.mtl.Tell

import cats.syntax.show.*
import cats.{Applicative, Functor, Monad, Show}

object mtl:
  given [F[_]: Console, L: Show](using Functor[F]): Tell[F, L] with
    val functor: Functor[F] = summon
    override inline def tell(l: L): F[Unit] = Console[F].println(l.show)
