package core.elevator

import cats.syntax.flatMap.*

import TestAppSuite.{*, given}

class SimulationSpec extends TestAppSuite:
  test("Simulation status can be changed") {
    runAppT(appEnv, appState) {
      val simulation = simpleSimulation[AppT]

      simulation.isRunning.assertEquals(false) >>
        simulation.start >> simulation.isRunning.assertEquals(true) >>
        simulation.stop >> simulation.isRunning.assertEquals(false)
    }
  }
