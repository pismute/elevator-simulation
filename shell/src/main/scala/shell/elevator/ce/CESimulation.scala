package shell.elevator.ce

import core.elevator.*

import scala.concurrent.duration.FiniteDuration

import cats.effect.Temporal
import cats.mtl.{Ask, Raise}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import classy.mtl.AtomicState

class CESimulation[F[_]](using
    F: Temporal[F],
    A: Ask[F, CESimulation.CESimulationEnv],
    S: AtomicState[F, Simulation.SimulationState]
) extends Simulation[F]:
  def sleepTick: F[Unit] =
    for
      tick <- A.reader(_.tick)
      _ <- F.sleep(tick)
    yield ()

object CESimulation:
  case class CESimulationEnv(tick: FiniteDuration)
