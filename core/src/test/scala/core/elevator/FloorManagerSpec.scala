package core.elevator

import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator.Elevator.*
import core.mtl.given
import core.test.*

import TestAppSuite.{*, given}

class FloorManagerSpec extends TestAppSuite:
  test("The floorManager can validate a floor") {
    runAppT(appEnv, appState) {
      val floorManager = simpleFloorManager[AppT]

      floorManager.checkFloor(appEnv.floorManagerEnv.lowestFloor - 1).assertEquals(false) *>
        floorManager.checkFloor(appEnv.floorManagerEnv.lowestFloor).assertEquals(true) *>
        floorManager.checkFloor(appEnv.floorManagerEnv.highestFloor + 1).assertEquals(false) *>
        floorManager.checkFloor(appEnv.floorManagerEnv.highestFloor).assertEquals(true)
    }
  }
