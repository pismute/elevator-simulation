package core

import cats.mtl.{Ask, Handle, Raise, Stateful}

import cats.syntax.applicativeError.*
import cats.syntax.monadError.*
import cats.{Applicative, Functor, Monad, MonadError}

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
