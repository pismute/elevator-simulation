package core.elevator

import cats.{Functor, Monad}
import cats.data.State
import cats.mtl.Stateful
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import classy.optics.Lens

// Analogous to cats.effect.Ref
trait AtomicState[F[_], A] extends Serializable:

  def get: F[A]

  def set(a: A): F[Unit]

  def getAndUpdate(f: A => A): F[A]

  def getAndSet(a: A): F[A]

  def updateAndGet(f: A => A): F[A]

  def update(f: A => A): F[Unit]

  def modify[B](f: A => (A, B)): F[B]

  def modifyState[B](state: State[A, B]): F[B]

  def tryUpdate(f: A => A): F[Boolean]

  def tryModify[B](f: A => (A, B)): F[Option[B]]

  def tryModifyState[B](state: State[A, B]): F[Option[B]]

end AtomicState

object AtomicState:

  /** It is only for test, AtomicState over Stateful does not make sense. Stateful is not atomic. So, Non-concurrent
    * environment must be guaranteed to run like in unit tests.
    */
  class UnsafeFromStateful[F[_]: Monad, A](parent: Stateful[F, A]) extends AtomicState[F, A]:

    def get: F[A] = parent.get

    def set(a: A): F[Unit] = parent.set(a)

    def getAndUpdate(f: A => A): F[A] = modify { a =>
      val b = f(a)
      (b, a)
    }

    def getAndSet(a: A): F[A] = modify(orig => (a, orig))

    def updateAndGet(f: A => A): F[A] = modify { a =>
      val b = f(a)
      (b, b)
    }

    def update(f: A => A): F[Unit] = parent.modify(f)

    def modify[B](f: A => (A, B)): F[B] =
      for
        a <- parent.get
        (a2, b) = f(a)
        _ <- parent.set(a2)
      yield b

    def modifyState[B](state: State[A, B]): F[B] = modify(a => state.run(a).value)

    def tryUpdate(f: A => A): F[Boolean] = tryModify(a => (f(a), ())).map(_.nonEmpty)

    def tryModify[B](f: A => (A, B)): F[Option[B]] = modify(f).map(Option.apply)

    def tryModifyState[B](state: State[A, B]): F[Option[B]] = tryModify(a => state.run(a).value)

  end UnsafeFromStateful

  class OpticAtomicState[F[_]: Functor, A, B](parent: AtomicState[F, A], lens: Lens[A, B]) extends AtomicState[F, B]:

    def get: F[B] = parent.get.map(lens.view)

    def set(b: B): F[Unit] = modify(_ => (b, ()))

    def getAndUpdate(f: B => B): F[B] = modify { b =>
      val c = f(b)
      (c, b)
    }

    def getAndSet(b: B): F[B] = modify(orig => (b, orig))

    def updateAndGet(f: B => B): F[B] = modify { b =>
      val c = f(b)
      (c, c)
    }

    def update(f: B => B): F[Unit] = modify(b => (f(b), ()))

    def modify[C](f: B => (B, C)): F[C] = parent.modify(a => lens.modify(a)(f))

    def modifyState[C](state: State[B, C]): F[C] =
      parent.modifyState[C](State(a => state.dimap(lens.view)(lens.set(a)).run(a).value))

    def tryUpdate(f: B => B): F[Boolean] = tryModify(b => (f(b), ())).map(_.nonEmpty)

    def tryModify[C](f: B => (B, C)): F[Option[C]] = parent.tryModify(a => lens.modify(a)(f))

    def tryModifyState[C](state: State[B, C]): F[Option[C]] =
      parent.tryModifyState[C](State(a => state.dimap(lens.view)(lens.set(a)).run(a).value))

  end OpticAtomicState

  def deriveAtomicState[F[_]: Functor, A, B](using parent: AtomicState[F, A], lens: Lens[A, B]): AtomicState[F, B] =
    OpticAtomicState(parent, lens)
end AtomicState
