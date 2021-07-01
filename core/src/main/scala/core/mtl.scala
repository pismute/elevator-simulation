package core

import cats.{Applicative, Functor, Monad}
import cats.MonadError
import cats.mtl.{Ask, Handle, Raise, Stateful}
import cats.syntax.applicativeError.*
import cats.syntax.monadError.*

object mtl:
  // optionOps's liftTo requires `ApplicativeError`.
  extension [F[_], A](oa: Option[A])
    def liftToT[E](ifEmpty: => E)(using F: Applicative[F], R: Raise[F, ? >: E]): F[A] =
      oa match {
        case Some(a) => F.pure(a)
        case None    => R.raise(ifEmpty)
      }

  class MkAsk[E0]:
    def apply[F[_], E](view: E0 => E)(using A: => Ask[F, E0]): Ask[F, E] = new Ask[F, E]:
      val applicative: Applicative[F] = A.applicative

      def ask[E2 >: E]: F[E2] = applicative.map(A.ask)(view)
  end MkAsk

  def mkAsk[E0]: MkAsk[E0] = MkAsk[E0]

  class MkRaise[E0, E]:
    def apply[F[_]](review: E => E0)(using R: Raise[F, E0]): Raise[F, E] =
      new Raise[F, E]:
        val functor: Functor[F] = R.functor

        def raise[E2 <: E, A](e: E2): F[A] = R.raise(review(e))

  end MkRaise

  def mkRaise[E0, E]: MkRaise[E0, E] = MkRaise[E0, E]

  class MkHandle[E0 <: Matchable, E]:
    def apply[F[_]](preview: PartialFunction[E0, E], review: E => E0)(using H: Handle[F, E0]): Handle[F, E] =
      new Handle[F, E]:
        val applicative: Applicative[F] = H.applicative

        def raise[E2 <: E, A](e: E2): F[A] = H.raise(review(e))

        def handleWith[A](fa: F[A])(f: E => F[A]): F[A] = H.handleWith(fa) {
          case preview(e) => f(e)
          case e          => H.raise(e)
        }

  end MkHandle

  def mkHandle[E0 <: Matchable, E]: MkHandle[E0, E] = MkHandle[E0, E]

  class MkStateful[S0]:
    def apply[F[_], S](view: S0 => S, setter: S => S0 => S0)(using R: Stateful[F, S0]): Stateful[F, S] =
      new Stateful[F, S]:
        val monad: Monad[F] = R.monad

        def get: F[S]          = monad.map(R.get)(view)
        def set(s: S): F[Unit] = R.modify(setter(s))
  end MkStateful

  def mkStateful[S0]: MkStateful[S0] = MkStateful[S0]
