package shell.elevator.ce

import cats.data.State
import cats.effect.Ref

import core.elevator.AtomicState

class AtomicStateFromRef[F[_], A](parent: Ref[F, A]) extends AtomicState[F, A]:

  def get: F[A] = parent.get

  def set(a: A): F[Unit] = parent.set(a)

  def getAndUpdate(f: A => A): F[A] = parent.getAndUpdate(f)

  def getAndSet(a: A): F[A] = parent.getAndSet(a)

  def updateAndGet(f: A => A): F[A] = parent.updateAndGet(f)

  def update(f: A => A): F[Unit] = parent.update(f)

  def modify[B](f: A => (A, B)): F[B] = parent.modify(f)

  def modifyState[B](state: State[A, B]): F[B] = parent.modifyState(state)

  def tryUpdate(f: A => A): F[Boolean] = parent.tryUpdate(f)

  def tryModify[B](f: A => (A, B)): F[Option[B]] = parent.tryModify(f)

  def tryModifyState[B](state: State[A, B]): F[Option[B]] = parent.tryModifyState(state)

end AtomicStateFromRef
