package shell.elevator.ce

import scala.concurrent.duration.DurationInt

import cats.{Applicative, Functor, Monad, Show}
import cats.effect.IO
import cats.mtl.Tell
import cats.syntax.apply.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*

import munit.*

import core.elevator.*
import core.test.*

import shell.elevator.ce.app.*
import shell.elevator.ce.appt.*
import shell.test.CatsEffectShellSuite

trait TestAppSuite extends CatsEffectShellSuite:
  export shell.elevator.ce.appt.io.*

  val appEnv = AppEnv(
    nrOfElevators = 2,
    duration = 3.seconds,
    floorManagerEnv = FloorManager.FloorManagerEnv(
      lowestFloor = -4,
      highestFloor = 5
    ),
    simulationEnv = CESimulation.CESimulationEnv(
      tick = 100.millis
    )
  )

  val appState = AppState[AppT](
    simulation = Simulation.SimulationState(),
    elevator = Map(),
    matrixSystem = CEMatrixSystem.CEMatrixSystemState(
      timestamps = Map()
    ),
    fiberSystem = CEFiberSystem.CEFiberSystemState[AppT](
      elevatorFibers = List(),
      passengerFibers = List()
    )
  )

  def runAppT[A](env: AppEnv)(f: => AppT[A])(using loc: Location): IO[A] =
    f.run // AppT
      .value // EitherT
      .map {
        case Left(err) => fail(show"$err")
        case Right(v)  => v
      }
      .run(env) // ReaderT

  // shut up the report in tests
  given [F[_], L: Show](using F: Applicative[F]): Tell[F, L] with
    val functor: Functor[F] = summon
    override inline def tell(l: L): F[Unit] = F.unit

end TestAppSuite
