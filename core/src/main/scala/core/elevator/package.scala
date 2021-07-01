package core.elevator

import cats.Show
import cats.derived.derived

import java.util.UUID

type ElevatorId = UUID
object ElevatorId:
  def random(): ElevatorId = UUID.randomUUID()

type Floor = Int
val Floor: Int.type = Int

type Distance = Int

type PassengerId = UUID
object PassengerId:
  def random(): ElevatorId = UUID.randomUUID()

case class Passenger(id: PassengerId, from: Floor, to: Floor) derives Show
object Passenger:
  def fromFloors(from: Floor, to: Floor): Passenger =
    Passenger(PassengerId.random(), from, to)

trait ElevatorAlg[F[_]]:
  def elevatorId: ElevatorId
  def distance(from: Floor): F[Option[Distance]]
  def call(floor: Floor): F[Unit]
  def getOn(passenger: Passenger): F[Unit]
  def start(): F[Unit]

trait FloorDoorsAlg[F[_]] {
  def await(floor: Floor): F[Unit]
  def awake(floor: Floor): F[Unit]
}

trait FloorManagerAlg[F[_]]:
  def waitOn(floor: Floor): F[Unit]
  def arrived(floor: Floor): F[Unit]
  def checkFloor(floor: Floor): F[Boolean]

trait SimulationAlg[F[_]]:
  def sleepTick(): F[Unit]
  def isRunning(): F[Boolean]
  def start(): F[Unit]
  def stop(): F[Unit]

trait SystemAlg[F[_]]:
  def elevators: List[ElevatorAlg[F]]
  def newPassenger(passenger: Passenger): F[Unit]
  def start(): F[Unit]
  def gracefully(): F[Unit]
