package shell.elevator.ce

import cats.effect.syntax.spawn.*
import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import core.elevator
import core.elevator.Elevator.*
import core.mtl.{*, given}
import core.test.*
import shell.elevator.ce.*
import shell.elevator.ce.app.{*, given}
import shell.elevator.ce.appt.*

class CEMainSpec extends TestAppSuite:
  test("A very simple simulation should run successfully") {
    runAppT(appEnv) {
      CEInterpreters.interpreters.use { interpreters =>
        CEMain.program(interpreters.system)
      }
    }
  }
