package shell.elevator.ce

import scala.concurrent.duration.FiniteDuration
import scala.util.NotGiven

import cats.{Functor, Show}
import cats.derived.derived
import cats.effect.Ref
import cats.mtl.{Ask, Raise}

import classy.effect.*
import classy.mtl.*

import core.elevator.*

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

  type AppError = Elevator.ElevatorError | System.SystemError | CEFloorDoors.CEFloorDoorsError

  inline given Show[AppError] = Show.show {
    case e: Elevator.ElevatorError         => summon[Show[Elevator.ElevatorError]].show(e)
    case e: System.SystemError             => summon[Show[System.SystemError]].show(e)
    case e: CEFloorDoors.CEFloorDoorsError => summon[Show[CEFloorDoors.CEFloorDoorsError]].show(e)
  }
