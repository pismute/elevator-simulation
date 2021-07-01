package shell.elevator.ce

import cats.effect.{Async, Deferred, Ref}
import cats.mtl.Raise
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator.*
import core.mtl.*

import scala.collection.immutable.IntMap

class CEFloorDoors[F[_]: Async](
  doors: Ref[F, IntMap[Deferred[F, Unit]]]
)(using
  R: Raise[F, CEFloorDoors.CEFloorDoorsError]
) extends FloorDoorsAlg[F]:
  def await(floor: Floor): F[Unit] =
    for
      map      <- doors.get
      deferred <- map.get(floor).liftToT(CEFloorDoors.CEFloorDoorsError.FloorNotFound(floor))
      _        <- deferred.get
    yield ()

  def awake(floor: Floor): F[Unit] =
    for
      maybeDeferred <- doors.modify { map =>
                         map.get(floor) match {
                           case None           => (map, None)
                           case Some(deferred) => (map + (floor -> Deferred.unsafe[F, Unit]), Some(deferred))
                         }
                       }
      deferred      <- maybeDeferred.liftToT(CEFloorDoors.CEFloorDoorsError.FloorNotFound(floor))
      _             <- deferred.complete(())
    yield ()

object CEFloorDoors:
  enum CEFloorDoorsError:
    case FloorNotFound(floor: Floor)

  def mkDoors[F[_]](low: Floor, high: Floor)(using F: Async[F]): F[Ref[F, IntMap[Deferred[F, Unit]]]] =
    for
      vs  <- F.replicateA(high - low + 1, F.defer(Deferred[F, Unit]))
      map  = IntMap.from((low to high).zip(vs))
      ref <- Ref.of(map)
    yield ref
