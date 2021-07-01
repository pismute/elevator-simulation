package core.elevator

import cats.{Eval, Monad, Show}
import cats.data.{EitherT, ReaderT, StateT}
import cats.derived.derived
import cats.mtl.{Ask, Handle, Raise, Stateful}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import core.mtl.*
import core.test.*
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
    f.run         // AppT
      .value      // EitherT
      .map {
        case Left(err) => fail(show"$err")
        case Right(v)  => v
      }
      .run(env)   // ReaderT
      .run(state) // StateT

end TestAppSuite

object TestAppSuite:
  case class AppEnv(floorManagerEnv: FloorManager.FloorManagerEnv)
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = mkAsk[AppEnv](_.floorManagerEnv)

  case class TestSimulationState(simulationState: Simulation.SimulationState, sleepCount: Int)
  given [F[_]](using Stateful[F, TestSimulationState]): Stateful[F, Simulation.SimulationState] =
    mkStateful[TestSimulationState](_.simulationState, s => parent => parent.copy(simulationState = s))

  case class AppState(testSimulationState: TestSimulationState, elevatorStates: Map[ElevatorId, Elevator.ElevatorState])
  given [F[_]](using Stateful[F, AppState]): Stateful[F, Map[ElevatorId, Elevator.ElevatorState]] =
    mkStateful[AppState](_.elevatorStates, s => parent => parent.copy(elevatorStates = s))
  given [F[_]](using Stateful[F, AppState]): Stateful[F, TestSimulationState]                     =
    mkStateful[AppState](_.testSimulationState, s => parent => parent.copy(testSimulationState = s))

  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)
  given [F[_]](using Handle[F, AppError]): Handle[F, Elevator.ElevatorError] =
    mkHandle[AppError, Elevator.ElevatorError](
      { case AppError.AppElevatorError(e) => e },
      AppError.AppElevatorError(_)
    )
  given [F[_]](using Handle[F, AppError]): Handle[F, System.SystemError]     =
    mkHandle[AppError, System.SystemError](
      { case AppError.AppSystemError(e) => e },
      AppError.AppSystemError(_)
    )

  object appt:
    private type App[A] = EitherT[ReaderT[StateT[Eval, AppState, *], AppEnv, *], AppError, A]
    opaque type AppT[A] = App[A]

    extension [A](appT: AppT[A])
      def run: App[A]      = appT
      def void: AppT[Unit] = appT.as(())

    object AppT:
      def apply[A](x: App[A]): AppT[A] = x

      given (using inst: Monad[App]): Monad[AppT]                           = inst
      given (using inst: Stateful[App, AppState]): Stateful[AppT, AppState] = inst
      given (using inst: Ask[App, AppEnv]): Ask[AppT, AppEnv]               = inst
      given (using inst: Handle[App, AppError]): Handle[AppT, AppError]     = inst

  export appt.*
