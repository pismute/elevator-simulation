package core.elevator

import cats.{Eval, Functor, Monad, Show}
import cats.data.{EitherT, ReaderT, StateT}
import cats.derived.derived
import cats.mtl.{Ask, Handle, Raise, Stateful}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*

import TestAppSuite.*
import munit.*

import classy.mtl.*
import classy.optics.*

import core.test.*

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
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = deriveAsk

  case class TestSimulationState(simulationState: Simulation.SimulationState, sleepCount: Int)
  given [F[_]: Functor, A](using AtomicState[F, TestSimulationState]): AtomicState[F, Simulation.SimulationState] =
    AtomicState.deriveAtomicState

  case class AppState(testSimulationState: TestSimulationState, elevatorStates: Map[ElevatorId, Elevator.ElevatorState])
  given [F[_]: Functor](using AtomicState[F, AppState]): AtomicState[F, TestSimulationState] =
    AtomicState.deriveAtomicState
  given [F[_]: Functor](using AtomicState[F, AppState]): AtomicState[F, Map[ElevatorId, Elevator.ElevatorState]] =
    AtomicState.deriveAtomicState

  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)
  given [F[_], A](using Handle[F, AppError], Prism[AppError, A]): Handle[F, A] = deriveHandle

  object appt:
    private type App[A] = EitherT[ReaderT[StateT[Eval, AppState, *], AppEnv, *], AppError, A]
    opaque type AppT[A] = App[A]

    extension [A](appT: AppT[A])
      def run: App[A] = appT
      def void: AppT[Unit] = appT.as(())

    object AppT:
      def apply[A](x: App[A]): AppT[A] = x

      given (using inst: Monad[App]): Monad[AppT] = inst
      given (using inst: Stateful[App, AppState]): AtomicState[AppT, AppState] = AtomicState.UnsafeFromStateful(inst)
      given (using inst: Ask[App, AppEnv]): Ask[AppT, AppEnv] = inst
      given (using inst: Handle[App, AppError]): Handle[AppT, AppError] = inst

  export appt.*
