package shell.elevator.ce

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.Random

import cats.effect.Async

import fs2.Stream

import core.elevator.*

object data:
  @tailrec
  private def randomFloors(low: Floor, high: Floor): (Int, Int) = {
    val pair = (Random.between(low, high + 1), Random.between(low, high + 1))

    if pair._1 == pair._2 then randomFloors(low, high) else pair
  }

  def newPassengers(low: Floor, high: Floor): Passenger = {
    val (from, to) = randomFloors(low, high)
    Passenger.fromFloors(from, to)
  }

  def passengers[F[_]](low: Floor, high: Floor, duration: FiniteDuration)(using F: Async[F]): Stream[F, Passenger] =
    Stream
      .repeatEval(F.delay(data.newPassengers(low, high)))
      .metered(1.seconds)
      .interruptAfter(duration)
