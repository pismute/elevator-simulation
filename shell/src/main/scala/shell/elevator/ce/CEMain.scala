package shell.elevator.ce

import scala.concurrent.duration.DurationInt

import cats.effect.{Async, IO, IOApp}
import cats.effect.std.Console
import cats.mtl.Ask
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import fs2.Stream

import core.elevator.*

import shell.elevator.ce.app.*
import shell.elevator.ce.appt.*
import shell.elevator.ce.mtl.given

object CEMain extends IOApp.Simple:
  val run = runApp(
    AppEnv(
      nrOfElevators = 2,
      //      duration = 1.minutes,
      duration = 10.seconds,
      floorManagerEnv = FloorManager.FloorManagerEnv(
        lowestFloor = -4,
        highestFloor = 18
      ),
      simulationEnv = CESimulation.CESimulationEnv(
        tick = 200.millis
      )
    )
  )

  // AppError is not an Exception.
  case class UncaughtAppError(error: AppError) extends Throwable

  def runApp(env: AppEnv): IO[Unit] =
    CEInterpreters
      .interpreters[AppT]
      .use(xs => program(xs.system))
      .run // AppT
      .leftMap(UncaughtAppError.apply)
      .rethrowT
      .run(env) // ReaderT

  def program[F[_]: Async: Console](system: SystemAlg[F])(using A: Ask[F, AppEnv]): F[Unit] =
    for
      env <- A.ask
      (duration, low, high) =
        (env.duration, env.floorManagerEnv.lowestFloor, env.floorManagerEnv.highestFloor)
      _ <- system.start
      input = data.passengers[F](low, high, duration)
      _ <- simulate(input, system)
      _ <- system.gracefully
    yield ()

  def simulate[F[_]: Async](input: Stream[F, Passenger], system: SystemAlg[F])(using C: Console[F]): F[Unit] =
    C.println("generating") >>
      input.foreach(x => system.newPassenger(x) *> C.println(".")).compile.drain >>
      C.println("done")
