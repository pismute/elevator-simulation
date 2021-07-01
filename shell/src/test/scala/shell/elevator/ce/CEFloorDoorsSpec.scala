package shell.elevator.ce

import cats.effect.syntax.spawn.*
import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator.Elevator.*
import core.mtl.{*, given}
import core.test.*
import shell.elevator.ce.*
import shell.elevator.ce.app.{*, given}
import shell.elevator.ce.appt.*
import shell.elevator.ce.mtl.*

class CEFloorDoorsSpec extends TestAppSuite:
  test("It should throw error if a requested floor does not exist".fail) {
    runAppT(appEnv) {
      CEInterpreters.floorDoors[AppT](1, 3).use { floorDoors =>
        floorDoors.await(0)
      }
    }
  }

  test("A fiber should await and be awaken") {
    runAppT(appEnv) {
      CEInterpreters.floorDoors[AppT](1, 3).use { floorDoors =>
        for
          fib1 <- floorDoors.await(1).start // lower boundary
          _    <- floorDoors.awake(1)
          _    <- fib1.join

          fib2 <- floorDoors.await(3).start // upper boundary
          _    <- floorDoors.awake(3)
          _    <- fib2.join
        yield ()
      }
    }
  }
