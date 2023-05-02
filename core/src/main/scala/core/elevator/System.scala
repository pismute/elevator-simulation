package core.elevator

import cats.{Monad, Order, Show}
import cats.derived.derived
import cats.mtl.Raise
import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.foldable.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import core.semigroups.*

import System.*

class System[F[_]](
  val elevators: List[ElevatorAlg[F]],
  floorManager: FloorManagerAlg[F],
  simulation: SimulationAlg[F]
)(using
  F: Monad[F],
  R: Raise[F, SystemError]
) extends SystemAlg[F]:
  def validate(passenger: Passenger): F[Unit] =
    floorManager
      .checkFloor(passenger.from)
      .flatMap(is => F.unlessA(is)(R.raise(SystemError.InvalidFromFloor(passenger.from)))) >>
      floorManager
        .checkFloor(passenger.to)
        .flatMap(is => F.unlessA(is)(R.raise(SystemError.InvalidToFloor(passenger.to))))

  def distanceElevator(floor: Floor): ElevatorAlg[F] => F[Option[DistanceElevator[F]]] =
    elevator => elevator.distance(floor).map(_.map(DistanceElevator[F](_, elevator)))

  def findClosest(floor: Floor): F[Option[ElevatorAlg[F]]] =
    elevators
      .traverse(distanceElevator(floor))
      .map(_.foldMap(_.map(Min(_))).map(_.get.elevator))

  // A passenger should hold a fiber. If a passenger waits for something, the fiber get suspended.
  // So it returns when the passenger got arrived the destination floor(passenger.to)
  def newPassenger(passenger: Passenger): F[Unit] =
    for
      _        <- validate(passenger)
      // keep trying to find if unavailable
      elevator <- findClosest(passenger.from).tailRecM(
                    _.map(_.toRight(simulation.sleepTick >> findClosest(passenger.from)))
                  )
      _        <- elevator.call(passenger.from)
      _        <- floorManager.waitOn(passenger.from)
      _        <- elevator.getOn(passenger)
    yield ()

  def start: F[Unit]      = simulation.start
  def gracefully: F[Unit] = simulation.stop *> floorManager.openAllDoors

object System:
  case class DistanceElevator[F[_]](distance: Distance, elevator: ElevatorAlg[F])
  object DistanceElevator:
    given [F[_]]: Order[DistanceElevator[F]] = Order.by(_.distance)

  enum SystemError derives CanEqual, Show:
    case InvalidFromFloor(floor: Floor)
    case InvalidToFloor(floor: Floor)
    case object NotRunning
