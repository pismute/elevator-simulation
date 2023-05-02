package shell.elevator.ce

import cats.effect.{Async, Deferred}
import cats.effect.std.AtomicCell
import cats.mtl.Raise
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator.*
import core.mtl.*

import scala.collection.immutable.IntMap

class CEFloorDoors[F[_]: Async](
  doors: AtomicCell[F, IntMap[Deferred[F, Unit]]]
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
      maybeDeferred <- doors.evalModify { dict =>
                         dict.get(floor) match {
                           case None           => (dict, None).pure
                           case Some(deferred) =>
                             Deferred[F, Unit].map { newDeferred =>
                               (dict + (floor -> newDeferred), Some(deferred))
                             }
                         }
                       }
      deferred      <- maybeDeferred.liftToT(CEFloorDoors.CEFloorDoorsError.FloorNotFound(floor))
      _             <- deferred.complete(())
    yield ()

object CEFloorDoors:
  enum CEFloorDoorsError:
    case FloorNotFound(floor: Floor)

  def mkDoors[F[_]](low: Floor, high: Floor)(using F: Async[F]): F[AtomicCell[F, IntMap[Deferred[F, Unit]]]] =
    for
      vs  <- F.replicateA(high - low + 1, Deferred[F, Unit])
      map  = IntMap.from((low to high).zip(vs))
      ref <- AtomicCell[F].of(map)
    yield ref
