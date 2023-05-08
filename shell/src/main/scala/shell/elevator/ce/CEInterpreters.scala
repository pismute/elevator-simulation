package shell.elevator.ce

import core.elevator.*
import core.mtl.*

import shell.elevator.ce.app.{*, given}
import shell.elevator.ce.mtl.*

import cats.{Monad, Parallel}
import cats.effect.{Async, Ref, Resource, Temporal}
import cats.effect.std.Console
import cats.mtl.{Ask, Raise, Tell}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import classy.ce3.all.*
import classy.mtl.AtomicState
import classy.mtl.all.{*, given}

final case class CEInterpreters[F[_]](
    simulation: SimulationAlg[F],
    floorManager: FloorManagerAlg[F],
    system: SystemAlg[F]
)

object CEInterpreters:
  def floorDoors[F[_]](low: Floor, high: Floor)(using
      F: Async[F],
      R: Raise[F, CEFloorDoors.CEFloorDoorsError]
  ): Resource[F, FloorDoorsAlg[F]] =
    for
      doors <- Resource.eval(CEFloorDoors.mkDoors(low, high))
      x = CEFloorDoors(doors)
    yield x

  def simulation[F[_]](using
      F: Temporal[F],
      A: Ask[F, CESimulation.CESimulationEnv],
      S: AtomicState[F, Simulation.SimulationState]
  ): Resource[F, SimulationAlg[F]] = Resource.pure(CESimulation())

  def floorManager[F[_]: Monad](floorDoors: FloorDoorsAlg[F])(using
      A: Ask[F, FloorManager.FloorManagerEnv]
  ): Resource[F, FloorManager[F]] = Resource.pure(FloorManager[F](floorDoors))

  def elevators[F[_]](
      floorDoors: FloorDoorsAlg[F],
      floorManager: FloorManager[F],
      simulation: SimulationAlg[F]
  )(using
      F: Monad[F],
      R: Raise[F, Elevator.ElevatorError],
      S: AtomicState[F, Map[ElevatorId, Elevator.ElevatorState]]
  ): Resource[F, List[ElevatorAlg[F]]] =
    Resource.eval(
      S.get.map { xs =>
        xs.keys.map(id => Elevator[F](id, floorDoors, floorManager, simulation)).toList
      }
    )

  def system[F[_]: Parallel: Console](
      elevators: List[ElevatorAlg[F]],
      floorManager: FloorManagerAlg[F],
      simulation: SimulationAlg[F]
  )(using
      F: Temporal[F],
      R: Raise[F, System.SystemError],
      S1: AtomicState[F, CEMatrixSystem.CEMatrixSystemState],
      S2: AtomicState[F, CEFiberSystem.CEFiberSystemState[F]],
      T: Tell[F, CEMatrixSystem.CEMatrixSystemReport]
  ): Resource[F, SystemAlg[F]] =
    Resource.pure {
      val system = System[F](elevators, floorManager, simulation)
      CEFiberSystem(CEMatrixSystem(system))
    }

  def interpreters[F[_]: Parallel: Console](using
      F: Async[F],
      A: Ask[F, AppEnv],
      R: Raise[F, AppError],
      T: Tell[F, CEMatrixSystem.CEMatrixSystemReport]
  ): Resource[F, CEInterpreters[F]] =
    for
      env <- Resource.eval(A.ask)
      stateRef <- appState(env)
      given AtomicState[F, AppState[F]] = stateRef.atomicState
      simulation <- simulation[F]
      doorsForFloor <- floorDoors[F](env.floorManagerEnv.lowestFloor, env.floorManagerEnv.highestFloor)
      floorManager <- floorManager[F](doorsForFloor)
      doorsForElevators <- floorDoors[F](env.floorManagerEnv.lowestFloor, env.floorManagerEnv.highestFloor)
      elevators <- elevators[F](doorsForElevators, floorManager, simulation)
      system <- system[F](elevators, floorManager, simulation)
    yield CEInterpreters(simulation, floorManager, system)

  def appState[F[_]: Async](env: AppEnv): Resource[F, Ref[F, AppState[F]]] = {
    val elevator = (0 until env.nrOfElevators).map { _ =>
      val id = ElevatorId.random()

      id -> Elevator.ElevatorState(id)
    }.toMap

    val matrixSystem = CEMatrixSystem.CEMatrixSystemState(
      timestamps = Map()
    )

    val fiberSystem = CEFiberSystem.CEFiberSystemState[F](
      elevatorFibers = List(),
      passengerFibers = List()
    )

    val appState = AppState[F](
      simulation = Simulation.SimulationState(),
      elevator = elevator,
      matrixSystem = matrixSystem,
      fiberSystem = fiberSystem
    )

    Resource.eval(Ref[F].of(appState))
  }
