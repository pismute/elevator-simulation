package core.elevator

import cats.{Applicative, Monad, Show}
import cats.mtl.{Ask, Handle, Raise, Stateful}
import cats.syntax.apply.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import TestAppSuite.{*, given}
import munit.Location

import classy.mtl.syntax.*

def dummyFloorDoors[F[_]](using F: Applicative[F]): FloorDoorsAlg[F] = new FloorDoorsAlg[F]:
  def await(floor: Floor): F[Unit] = F.unit
  def awake(floor: Floor): F[Unit] = F.unit
  def awakeAll: F[Unit] = F.unit

def simpleSimulation[F[_]](using
    F: Monad[F],
    S: AtomicState[F, TestSimulationState]
): SimulationAlg[F] =
  new Simulation[F]:
    // An elevator is designed to hold a fiber to run until simulation get stopped
    // core module does not depend on an effect system, so it holds a thread.
    // a test keeps getting blocked if simulation keeps running
    // Every iteration of Elevator calls `sleepTick`, if it called sleepCount times, the simulation get stopped
    def sleepTick: F[Unit] =
      S.update { s =>
        if s.sleepCount > 0 then s.copy(sleepCount = s.sleepCount - 1)
        else s.copy(simulationState = Simulation.SimulationState())
      }

def simpleFloorManager[F[_]: Monad](using
    A: Ask[F, FloorManager.FloorManagerEnv]
): FloorManagerAlg[F] = new FloorManager[F](dummyFloorDoors)

def mkSimpleElevators[F[_]](
    floorManager: FloorManagerAlg[F],
    simulation: SimulationAlg[F]
)(using
    F: Monad[F],
    R: Raise[F, Elevator.ElevatorError],
    S: AtomicState[F, Map[ElevatorId, Elevator.ElevatorState]]
): F[List[ElevatorAlg[F]]] =
  S.get.map { xs =>
    xs.keys.map(id => new Elevator[F](id, dummyFloorDoors, floorManager, simulation)).toList
  }

def simpleSystem[F[_]](
    elevators: List[ElevatorAlg[F]],
    floorManager: FloorManagerAlg[F],
    simulation: SimulationAlg[F]
)(using
    F: Monad[F],
    R: Raise[F, System.SystemError]
): System[F] = new System[F](elevators, floorManager, simulation)

def mkSimpleSystem[F[_]](using
    F: Monad[F],
    A: Ask[F, AppEnv],
    R: Handle[F, AppError],
    S: AtomicState[F, AppState]
): F[System[F]] = {
  val floorManager = simpleFloorManager[F]
  val simulation = simpleSimulation[F]

  for
    elevators <- mkSimpleElevators[F](floorManager, simulation)
    system = simpleSystem[F](elevators, floorManager, simulation)
  yield system
}
