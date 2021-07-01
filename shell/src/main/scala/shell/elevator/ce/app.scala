package shell.elevator.ce

import cats.{Eval, Monad, Show}
import cats.derived.derived
import cats.mtl.{Ask, Handle, Raise, Stateful}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import core.elevator.*
import core.mtl.*

import scala.concurrent.duration.FiniteDuration

object app:
  case class AppEnv(
    nrOfElevators: Int,
    duration: FiniteDuration,
    floorManagerEnv: FloorManager.FloorManagerEnv,
    simulationEnv: CESimulation.CESimulationEnv
  )
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = mkAsk[AppEnv](_.floorManagerEnv)
  given [F[_]](using Ask[F, AppEnv]): Ask[F, CESimulation.CESimulationEnv] = mkAsk[AppEnv](_.simulationEnv)

  case class AppState[F[_]](
    simulation: Simulation.SimulationState,
    elevator: Map[ElevatorId, Elevator.ElevatorState],
    matrixSystem: CEMatrixSystem.CEMatrixSystemState,
    fiberSystem: CEFiberSystem.CEFiberSystemState[F]
  )
  given [F[_]](using Stateful[F, AppState[F]]): Stateful[F, Map[ElevatorId, Elevator.ElevatorState]] =
    mkStateful[AppState[F]](_.elevator, s => parent => parent.copy(elevator = s))
  given [F[_]](using Stateful[F, AppState[F]]): Stateful[F, Simulation.SimulationState]              =
    mkStateful[AppState[F]](_.simulation, s => parent => parent.copy(simulation = s))
  given [F[_]](using Stateful[F, AppState[F]]): Stateful[F, CEMatrixSystem.CEMatrixSystemState]      =
    mkStateful[AppState[F]](_.matrixSystem, s => parent => parent.copy(matrixSystem = s))
  given [F[_]](using Stateful[F, AppState[F]]): Stateful[F, CEFiberSystem.CEFiberSystemState[F]]     =
    mkStateful[AppState[F]](_.fiberSystem, s => parent => parent.copy(fiberSystem = s))

  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)
    case AppCEFloorDoors(error: CEFloorDoors.CEFloorDoorsError)
  given [F[_]](using Raise[F, AppError]): Raise[F, Elevator.ElevatorError]         =
    mkRaise[AppError, Elevator.ElevatorError](AppError.AppElevatorError(_))
  given [F[_]](using Raise[F, AppError]): Raise[F, System.SystemError]             =
    mkRaise[AppError, System.SystemError](AppError.AppSystemError(_))
  given [F[_]](using Raise[F, AppError]): Raise[F, CEFloorDoors.CEFloorDoorsError] =
    mkRaise[AppError, CEFloorDoors.CEFloorDoorsError](AppError.AppCEFloorDoors(_))
