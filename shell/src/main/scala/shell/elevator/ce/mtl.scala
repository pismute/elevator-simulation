package shell.elevator.ce

import cats.{Applicative, Functor, Monad, Show}
import cats.effect.Ref
import cats.effect.std.Console
import cats.mtl.{Stateful, Tell}
import cats.syntax.show.*

object mtl:
  extension [F[_], S](ref: Ref[F, S])
    def stateful(using Monad[F]): Stateful[F, S] =
      new Stateful[F, S]:
        val monad: Monad[F]                             = summon
        override inline def get: F[S]                   = ref.get
        override inline def set(s: S): F[Unit]          = ref.set(s)
        override inline def inspect[A](f: S => A): F[A] = monad.map(get)(f)
        override inline def modify(f: S => S): F[Unit]  = ref.update(f)

  given [F[_]: Console, L: Show](using Functor[F]): Tell[F, L] with
    val functor: Functor[F]                 = summon
    override inline def tell(l: L): F[Unit] = Console[F].println(l.show)
