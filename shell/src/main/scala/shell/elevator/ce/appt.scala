package shell.elevator.ce

import cats.Parallel
import cats.data.{EitherT, ReaderT}
import cats.effect.{Async, IO, LiftIO}
import cats.effect.std.Console
import cats.effect.syntax.*
import cats.mtl.{Ask, Raise, Stateful}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import core.elevator.*

import shell.elevator.ce.app.*

object appt:
  private type App[A] = EitherT[ReaderT[IO, AppEnv, *], AppError, A]
  opaque type AppT[A] = App[A]

  extension [A](appT: AppT[A])
    def run: App[A] = appT
    def void: AppT[Unit] = appT.as(())

  object AppT:
    def apply[A](x: App[A]): AppT[A] = x

    given (using inst: Ask[App, AppEnv]): Ask[AppT, AppEnv] = inst
    given (using inst: Async[App]): Async[AppT] = inst
    given (using inst: Console[App]): Console[AppT] = inst
    given (using inst: LiftIO[App]): LiftIO[AppT] = inst
    given (using inst: Parallel[App]): Parallel[AppT] = inst
    given (using inst: Raise[App, AppError]): Raise[AppT, AppError] = inst

  object io:
    extension [A](ioa: IO[A]) def liftTo[F[_]](using lio: LiftIO[F]): F[A] = lio.liftIO(ioa)
