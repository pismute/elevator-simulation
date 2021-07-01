package shell.elevator.ce

import cats.Parallel
import cats.derived.{ShowPretty, derived}
import cats.effect.{Fiber, Spawn, Temporal}
import cats.effect.implicits.*
import cats.instances.list.*
import cats.mtl.{Ask, Raise, Stateful, Tell}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.parallel.*
import cats.syntax.show.*
import cats.syntax.traverse.*
import core.elevator.*
import org.typelevel.cats.time.*

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class CEMatrixSystem[F[_]](inner: SystemAlg[F])(using
  F: Temporal[F],
  S: Stateful[F, CEMatrixSystem.CEMatrixSystemState],
  T: Tell[F, CEMatrixSystem.CEMatrixSystemReport]
) extends SystemAlg[F]:
  def elevators: List[ElevatorAlg[F]] = inner.elevators

  def newPassenger(passenger: Passenger): F[Unit] =
    for
      before <- F.monotonic
      _      <- S.modify(CEMatrixSystem.enter(passenger, before))
      _      <- inner.newPassenger(passenger)
      after  <- F.monotonic
      _      <- S.modify(CEMatrixSystem.leave(passenger, after))
    yield ()

  def start(): F[Unit]      = inner.start()
  def gracefully(): F[Unit] = inner.gracefully() >> report()

  def report(): F[Unit] =
    for
      state <- S.get
      r      = CEMatrixSystem.genReport(state)
      _     <- T.tell(r.get)
    yield ()

object CEMatrixSystem:
  case class TimeStamp(enter: FiniteDuration, leave: Option[FiniteDuration], move: Int)
  case class CEMatrixSystemState(timestamps: Map[PassengerId, TimeStamp])
  case class CEMatrixSystemReport(
    avgDuration: FiniteDuration,
    avgMove: Double,
    firstEnter: FiniteDuration,
    firstLeave: FiniteDuration,
    lastEnter: FiniteDuration,
    lastLeave: FiniteDuration,
    maxMove: Int,
    minDuration: FiniteDuration,
    minMove: Int,
    maxDuration: FiniteDuration,
    nrOfPassengers: Long,
    sumOfDuration: FiniteDuration,
    sumOfMove: Long
  ) derives ShowPretty

  def enter(passenger: Passenger, now: FiniteDuration): CEMatrixSystemState => CEMatrixSystemState =
    state =>
      state.copy(
        timestamps = state.timestamps + (passenger.id -> TimeStamp(
          enter = now,
          leave = None,
          move = (passenger.from - passenger.to).abs
        ))
      )

  def leave(passenger: Passenger, now: FiniteDuration): CEMatrixSystemState => CEMatrixSystemState =
    state => {
      val timestamps = state.timestamps.updatedWith(passenger.id)(_.map(_.copy(leave = Some(now))))
      state.copy(
        timestamps = timestamps
      )
    }

  val MinDuration  = FiniteDuration(0L, TimeUnit.NANOSECONDS)
  val MaxDuration  = FiniteDuration(Long.MaxValue, TimeUnit.NANOSECONDS)
  val ZeroDuration = FiniteDuration(0L, TimeUnit.NANOSECONDS)
  val ZeroMove     = 0L

  def genReport(state: CEMatrixSystemState): Option[CEMatrixSystemReport] = {

    val init = CEMatrixSystemReport(
      avgDuration = ZeroDuration,
      avgMove = 0d,
      firstEnter = MaxDuration,
      firstLeave = MinDuration,
      lastEnter = MinDuration,
      lastLeave = MaxDuration,
      maxDuration = MinDuration,
      maxMove = Int.MinValue,
      minDuration = MaxDuration,
      minMove = Int.MaxValue,
      nrOfPassengers = 0L,
      sumOfDuration = ZeroDuration,
      sumOfMove = ZeroMove
    )

    val report = state.timestamps.values.foldLeft(Option(init)) { (acc, x) =>
      for
        r       <- acc
        leave   <- x.leave
        duration = leave - x.enter
      yield r.copy(
        firstEnter = r.firstEnter.min(x.enter),
        firstLeave = r.firstLeave.min(leave),
        lastEnter = r.lastEnter.max(x.enter),
        lastLeave = r.lastLeave.max(leave),
        maxDuration = r.maxDuration.max(duration),
        maxMove = r.minMove.max(x.move),
        minDuration = r.minDuration.min(duration),
        minMove = r.minMove.min(x.move),
        nrOfPassengers = r.nrOfPassengers + 1,
        sumOfDuration = r.sumOfDuration + duration,
        sumOfMove = r.sumOfMove + x.move
      )
    }

    report.map(x =>
      x.copy(
        avgDuration = x.sumOfDuration / x.nrOfPassengers,
        avgMove = x.sumOfMove / x.nrOfPassengers.toDouble
      )
    )
  }

class CEFiberSystem[F[_]: Parallel](inner: SystemAlg[F])(using
  F: Spawn[F],
  S: Stateful[F, CEFiberSystem.CEFiberSystemState[F]]
) extends SystemAlg[F]:
  def elevators: List[ElevatorAlg[F]] = inner.elevators

  def newPassenger(passenger: Passenger): F[Unit] =
    for
      fib <- inner.newPassenger(passenger).start
      _   <- S.modify(x => x.copy(passengerFibers = fib :: x.passengerFibers))
    yield ()

  def start(): F[Unit] =
    for
      _      <- inner.start()
      fibers <- elevators.traverse(_.start().start)
      _      <- S.modify(x => x.copy(elevatorFibers = fibers))
    yield ()

  def gracefully(): F[Unit] =
    for
      state <- S.get
      _     <- state.passengerFibers.parTraverse(_.join)
      _     <- inner.gracefully()
      _     <- state.elevatorFibers.parTraverse(_.join)
    yield ()

object CEFiberSystem:
  case class CEFiberSystemState[F[_]](
    elevatorFibers: List[Fiber[F, Throwable, Unit]],
    passengerFibers: List[Fiber[F, Throwable, Unit]]
  )
