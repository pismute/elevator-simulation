package shell.elevator.ce

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.syntax.spawn.*
import cats.effect.testkit.TestControl
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import shell.elevator.ce.appt.*

class CEFloorDoorsSpec extends TestAppSuite:
  test("It should throw error if a requested floor does not exist".fail) {
    runAppT(appEnv) {
      CEInterpreters.floorDoors[AppT](1, 3).use { floorDoors =>
        floorDoors.await(0)
      }
    }
  }

  test("A fiber should await and be awaken: lower boundary") {
    TestControl.executeEmbed {
      runAppT(appEnv) {
        CEInterpreters.floorDoors[AppT](1, 3).use { floorDoors =>
          for
            fib <- floorDoors.await(1).start // lower bound
            // awake must be called after await
            _ <- IO.sleep(100.milli).liftTo[AppT]
            _ <- floorDoors.awake(1)
            _ <- fib.join
          yield ()
        }
      }
    }
  }

  test("A fiber should await and be awaken: upper boundary") {
    TestControl.executeEmbed {
      runAppT(appEnv) {
        CEInterpreters.floorDoors[AppT](1, 3).use { floorDoors =>
          for
            fib <- floorDoors.await(3).start // upper boundary
            // awake must be called after await
            _ <- IO.sleep(100.milli).liftTo[AppT]
            _ <- floorDoors.awake(3)
            _ <- fib.join
          yield ()
        }
      }
    }
  }
