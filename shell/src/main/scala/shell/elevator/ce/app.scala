package shell.elevator.ce

import scala.concurrent.duration.FiniteDuration

import cats.mtl.{Ask, Raise}

import cats.derived.derived
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import cats.{Eval, Monad, Show}

import core.elevator.*
import core.mtl.*

import classy.mtl.AtomicState
import classy.mtl.all.{*, given}
import classy.optics.optics

object app:
  case class AppEnv(
      nrOfElevators: Int,
      duration: FiniteDuration,
      floorManagerEnv: FloorManager.FloorManagerEnv,
      simulationEnv: CESimulation.CESimulationEnv
  )
  given [F[_]](using Ask[F, AppEnv]): Ask[F, FloorManager.FloorManagerEnv] = mkAsk[AppEnv](_.floorManagerEnv)
  given [F[_]](using Ask[F, AppEnv]): Ask[F, CESimulation.CESimulationEnv] = mkAsk[AppEnv](_.simulationEnv)

  @optics
  case class AppState[F[_]](
      simulation: Simulation.SimulationState,
      elevator: Map[ElevatorId, Elevator.ElevatorState],
      matrixSystem: CEMatrixSystem.CEMatrixSystemState,
      fiberSystem: CEFiberSystem.CEFiberSystemState[F]
  )

  @optics
  enum AppError derives Show:
    case AppElevatorError(error: Elevator.ElevatorError)
    case AppSystemError(error: System.SystemError)
    case AppCEFloorDoors(error: CEFloorDoors.CEFloorDoorsError)
