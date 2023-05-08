package core.elevator

import cats.mtl.{Ask, Handle, Raise, Stateful}

import cats.data.{EitherT, ReaderT, StateT}
import cats.derived.derived
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import cats.{Eval, Monad, Show}

import core.mtl.*
import core.test.*

import classy.mtl.AtomicState
import classy.mtl.all.{*, given}
import classy.optics.optics
import munit.*

import TestAppSuite.*

trait TestAppSuite extends CoreSuite:
  val appEnv = AppEnv(
    floorManagerEnv = FloorManager.FloorManagerEnv(
      lowestFloor = 0,
      highestFloor = 5
    )
  )

  val appState = AppState(
    testSimulationState = TestSimulationState(simulationState = Simulation.SimulationState(), sleepCount = 10),
    elevatorStates = {
      val elevatorId = ElevatorId.random()
      Map(
        elevatorId -> Elevator.ElevatorState(
          id = elevatorId,
          floor = 3,
          destinations = List.empty
        )
      )
    }
  )

  def runAppT[A](env: AppEnv, state: AppState)(f: => AppT[A])(using loc: Location): Eval[(AppState, A)] =
    f.run // AppT
      .value // EitherT
      .map {
        case Left(err) => fail(show"$err")
        case Right(v)  => v
      }
      .run(env) // ReaderT
      .run(state) // StateT

end TestAppSuite

object TestAppSuite:
  case class AppEnv(floorManagerEnv: FloorManager.FloorManagerEnv)
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = mkAsk[AppEnv](_.floorManagerEnv)

  @optics
  case class TestSimulationState(simulationState: Simulation.SimulationState, sleepCount: Int)

  @optics
  case class AppState(testSimulationState: TestSimulationState, elevatorStates: Map[ElevatorId, Elevator.ElevatorState])

  @optics
  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)

  object appt:
    private type App[A] = EitherT[ReaderT[StateT[Eval, AppState, *], AppEnv, *], AppError, A]
    opaque type AppT[A] = App[A]

    extension [A](appT: AppT[A])
      def run: App[A] = appT
      def void: AppT[Unit] = appT.as(())

    object AppT:
      def apply[A](x: App[A]): AppT[A] = x

      given (using inst: Monad[App]): Monad[AppT] = inst
      given (using inst: Stateful[App, AppState]): AtomicState[AppT, AppState] = inst.unsafeAtomicState
      given (using inst: Ask[App, AppEnv]): Ask[AppT, AppEnv] = inst
      given (using inst: Handle[App, AppError]): Handle[AppT, AppError] = inst

  export appt.*
