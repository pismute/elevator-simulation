package core.elevator

import cats.{Monad, Show}
import cats.derived.derived
import cats.mtl.Raise
import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*

import Elevator.*

import classy.mtl.*

class Elevator[F[_]](
    val elevatorId: ElevatorId,
    floorDoors: FloorDoorsAlg[F],
    floorManager: FloorManagerAlg[F],
    simulation: SimulationAlg[F]
)(using
    F: Monad[F],
    R: Raise[F, ElevatorError],
    S: AtomicState[F, Map[ElevatorId, ElevatorState]]
) extends ElevatorAlg[F]:
  private def getState: F[ElevatorState] = S.get.flatMap(_.get(elevatorId).liftTo(ElevatorError.StateNotFound))

  private def modify[B](f: ElevatorState => (ElevatorState, B)): F[B] =
    S.modify { all =>
      all.get(elevatorId) match
        case None => (all, None)
        case Some(state) =>
          val (s, b) = f(state)
          (all + (elevatorId -> s), Some(b))
    }.flatMap(_.liftTo(ElevatorError.StateNotFound))

  def distance(from: Floor): F[Option[Distance]] =
    getState.map(ElevatorState.distance(from))

  def call(floor: Floor): F[Unit] =
    modify(s => ElevatorState.call(floor)(s) -> ())

  def getOn(passenger: Passenger): F[Unit] =
    modify(s => ElevatorState.getOn(passenger)(s) -> ()) *>
      floorDoors.await(passenger.to)

  def start: F[Unit] = {
    val move =
      for
        next <- modify { s =>
          val next = ElevatorState.move1Floor(s)
          (next.state, next)
        }
        _ <- F.whenA(next.arrived) {
          floorManager.arrived(next.state.floor) *> floorDoors.awake(next.state.floor)
        }
      yield ()

    F.tailRecM(())(_ => simulation.isRunning.ifM(simulation.sleepTick >> move.map(_.asLeft), ().asRight.pure))
  }
end Elevator

object Elevator:
  enum Direction(int: Int) derives CanEqual:
    val asInt: Int = int

    case Up extends Direction(1)
    case Down extends Direction(-1)
    case Wait extends Direction(0)

  object Direction:
    def fromFloor(from: Floor, to: Floor): Direction =
      if from < to then Up
      else if from > to then Down
      else Wait

  end Direction

  enum Destination(val floor: Floor) derives CanEqual:
    case PassengerDestination(passenger: Passenger) extends Destination(passenger.to)
    case CallDestination(override val floor: Floor) extends Destination(floor)

  case class ElevatorState(
      id: ElevatorId,
      floor: Floor,
      destinations: List[Destination]
  )
  object ElevatorState:
    def apply(id: ElevatorId): ElevatorState = apply(id, 0, List())

    def direction: ElevatorState => Direction =
      state =>
        state.destinations.headOption match
          case None           => Direction.Wait
          case Some(waypoint) => Direction.fromFloor(state.floor, waypoint.floor)

    def distance(from: Floor): ElevatorState => Option[Distance] =
      state =>
        (direction(state), state.floor - from) match
          case (Direction.Wait, d)          => d.abs.some
          case (Direction.Up, d) if d < 0   => d.abs.some
          case (Direction.Down, d) if d > 0 => d.abs.some
          case _                            => None // opposite direction

    def call(floor: Floor): ElevatorState => ElevatorState =
      state => state.copy(destinations = Destination.CallDestination(floor) +: state.destinations)

    def getOn(passenger: Passenger): ElevatorState => ElevatorState =
      state => state.copy(destinations = Destination.PassengerDestination(passenger) +: state.destinations)

    case class AfterMoving(
        state: ElevatorState,
        direction: Direction,
        arrived: Boolean
    )
    def move1Floor(state: ElevatorState): AfterMoving =
      val direct = direction(state)
      val nextFloor = state.floor + direct.asInt

      val (leaving, stay) = state.destinations.partition(_.floor == nextFloor)
      AfterMoving(state.copy(floor = nextFloor, destinations = stay), direct, leaving.nonEmpty)

  end ElevatorState

  enum ElevatorError derives Show:
    case StateNotFound
end Elevator
