package shell.elevator.ce

class CEMainSpec extends TestAppSuite:
  test("A very simple simulation should run successfully") {
    runAppT(appEnv) {
      CEInterpreters.interpreters.use { interpreters =>
        CEMain.program(interpreters.system)
      }
    }
  }
