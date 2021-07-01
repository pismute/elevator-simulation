package core.elevator

import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.mtl.given
import core.test.*

import TestAppSuite.{*, given}

class SimulationSpec extends TestAppSuite:
  test("Simulation status can be changed") {
    runAppT(appEnv, appState) {
      val simulation = simpleSimulation[AppT]

      simulation.isRunning().assertEquals(false) >>
        simulation.start() >> simulation.isRunning().assertEquals(true) >>
        simulation.stop() >> simulation.isRunning().assertEquals(false)
    }
  }
