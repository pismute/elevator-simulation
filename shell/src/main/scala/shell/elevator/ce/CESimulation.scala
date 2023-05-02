package shell.elevator.ce

import cats.effect.Temporal
import cats.mtl.{Ask, Raise, Stateful}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator.*

import scala.concurrent.duration.FiniteDuration

class CESimulation[F[_]](using
  F: Temporal[F],
  A: Ask[F, CESimulation.CESimulationEnv],
  S: Stateful[F, Simulation.SimulationState]
) extends Simulation[F]:
  def sleepTick: F[Unit] =
    for
      tick <- A.reader(_.tick)
      _    <- F.sleep(tick)
    yield ()

object CESimulation:
  case class CESimulationEnv(tick: FiniteDuration)
