package core.elevator

import cats.{Eval, Functor, Monad, Show}
import cats.data.{EitherT, ReaderT, StateT}
import cats.mtl.{Ask, Handle, Stateful}
import cats.syntax.functor.*
import cats.syntax.show.*

import TestAppSuite.{*, given}
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

  type AppError = Elevator.ElevatorError | System.SystemError

  given Show[AppError] = Show.show {
    case e: Elevator.ElevatorError => summon[Show[Elevator.ElevatorError]].show(e)
    case e: System.SystemError     => summon[Show[System.SystemError]].show(e)
  }

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
