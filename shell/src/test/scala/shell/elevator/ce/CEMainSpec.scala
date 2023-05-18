package shell.elevator.ce

import cats.effect.testkit.TestControl

class CEMainSpec extends TestAppSuite:
  test("A very simple simulation should run successfully") {
    TestControl.executeEmbed {
      runAppT(appEnv) {
        CEInterpreters.interpreters.use { interpreters =>
          CEMain.program(interpreters.system)
        }
      }
    }
  }
