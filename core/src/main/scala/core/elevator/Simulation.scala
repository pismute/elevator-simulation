package core.elevator

import cats.{Monad, Show}
import cats.derived.derived
import cats.mtl.{Raise, Stateful}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import Simulation.*

abstract class Simulation[F[_]: Monad](using
  S: Stateful[F, SimulationState]
) extends SimulationAlg[F] {
  def isRunning(): F[Boolean] = S.get.map(_.status == SimulationStatus.Running)
  def start(): F[Unit]        = S.modify(_.copy(status = SimulationStatus.Running))
  def stop(): F[Unit]         = S.modify(_.copy(status = SimulationStatus.Stopped))
}

object Simulation:
  case class SimulationState(status: SimulationStatus = SimulationStatus.Stopped)

  enum SimulationStatus derives Show, CanEqual:
    case Running
    case Stopped
