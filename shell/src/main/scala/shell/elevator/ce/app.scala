package shell.elevator.ce

import scala.concurrent.duration.FiniteDuration
import scala.util.NotGiven

import cats.effect.Ref

import cats.mtl.{Ask, Raise}

import cats.derived.derived
import cats.{Functor, Show}

import core.elevator.*

import classy.effect.*
import classy.mtl.*

object app:
  case class AppEnv(
      nrOfElevators: Int,
      duration: FiniteDuration,
      floorManagerEnv: FloorManager.FloorManagerEnv,
      simulationEnv: CESimulation.CESimulationEnv
  )
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = deriveAsk
  given [F[_]](using Ask[F, AppEnv]): Ask[F, CESimulation.CESimulationEnv] = deriveAsk

  case class AppState[F[_]](
      simulation: Simulation.SimulationState,
      elevator: Map[ElevatorId, Elevator.ElevatorState],
      matrixSystem: CEMatrixSystem.CEMatrixSystemState,
      fiberSystem: CEFiberSystem.CEFiberSystemState[F]
  )
  given [F[_]: Functor](using Ref[F, AppState[F]]): Ref[F, Simulation.SimulationState] = deriveRef
  given [F[_]: Functor](using Ref[F, AppState[F]]): Ref[F, Map[ElevatorId, Elevator.ElevatorState]] = deriveRef
  given [F[_]: Functor](using Ref[F, AppState[F]]): Ref[F, CEMatrixSystem.CEMatrixSystemState] = deriveRef
  given [F[_]: Functor](using Ref[F, AppState[F]]): Ref[F, CEFiberSystem.CEFiberSystemState[F]] = deriveRef
  given [F[_], A](using NotGiven[AtomicState[F, A]], Ref[F, A]): AtomicState[F, A] = AtomicStateFromRef(summon)

  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)
    case AppCEFloorDoors(error: CEFloorDoors.CEFloorDoorsError)
  given [F[_]](using Raise[F, AppError]): Raise[F, Elevator.ElevatorError] = deriveRaise
  given [F[_]](using Raise[F, AppError]): Raise[F, System.SystemError] = deriveRaise
  given [F[_]](using Raise[F, AppError]): Raise[F, CEFloorDoors.CEFloorDoorsError] = deriveRaise
