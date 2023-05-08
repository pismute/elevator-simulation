package core.elevator

import cats.mtl.Ask

import cats.derived.derived
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Monad, Show}

import FloorManager.*

class FloorManager[F[_]: Monad](
  floorDoors: FloorDoorsAlg[F]
)(using
  A: Ask[F, FloorManagerEnv]
) extends FloorManagerAlg[F]:
  def waitOn(floor: Floor): F[Unit] = floorDoors.await(floor)

  def arrived(floor: Floor): F[Unit] = floorDoors.awake(floor)

  def checkFloor(floor: Floor): F[Boolean] =
    for env <- A.ask
    yield env.lowestFloor <= floor && floor <= env.highestFloor

  def openAllDoors: F[Unit] = floorDoors.awakeAll

object FloorManager:
  case class FloorManagerEnv(
    lowestFloor: Floor,
    highestFloor: Floor
  ) derives Show
