package core.elevator

import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import TestAppSuite.{*, given}

import core.test.*

class SystemSpec extends TestAppSuite:
  test("System can validate a passenger") {
    runAppT(appEnv, appState) {
      for
        system <- mkSimpleSystem[AppT]
        _ <-
          system.validate(
            Passenger.fromFloors(
              appEnv.floorManagerEnv.lowestFloor,
              appEnv.floorManagerEnv.highestFloor
            )
          )
      yield ()
    }
  }

  test("System should reject if a passenger has started from an invalid floor".fail) {
    runAppT(appEnv, appState) {
      for
        system <- mkSimpleSystem[AppT]
        _ <-
          system.validate(
            Passenger.fromFloors(
              appEnv.floorManagerEnv.lowestFloor - 1,
              appEnv.floorManagerEnv.highestFloor
            )
          )
      yield ()
    }
  }

  test("System should reject if a passenger has started an invalid destination".fail) {
    runAppT(appEnv, appState) {
      for
        system <- mkSimpleSystem[AppT]
        _ <-
          system.validate(
            Passenger.fromFloors(
              appEnv.floorManagerEnv.lowestFloor,
              appEnv.floorManagerEnv.highestFloor + 1
            )
          )
      yield ()
    }
  }

  test("System should reject if a passenger has started an invalid destination".fail) {
    runAppT(appEnv, appState) {
      for
        system <- mkSimpleSystem[AppT]
        _ <-
          system.validate(
            Passenger.fromFloors(
              appEnv.floorManagerEnv.lowestFloor,
              appEnv.floorManagerEnv.highestFloor + 1
            )
          )
      yield ()
    }
  }

  test("System should find the closest elevator") {
    val elevatorId1 = ElevatorId.random()
    val elevatorId2 = ElevatorId.random()
    val twoElevators = AppState(
      testSimulationState = TestSimulationState(simulationState = Simulation.SimulationState(), sleepCount = 10),
      elevatorStates = Map(
        elevatorId1 -> Elevator.ElevatorState(
          id = elevatorId1,
          floor = 2,
          destinations = List.empty
        ),
        elevatorId2 -> Elevator.ElevatorState(
          id = elevatorId2,
          floor = 5,
          destinations = List.empty
        )
      )
    )

    runAppT(appEnv, twoElevators) {
      for
        system <- mkSimpleSystem[AppT]
        elevator <- system.findClosest(1)
        _ = elevator.map(_.elevatorId).assertEquals(elevatorId1)
        elevator2 <- system.findClosest(4)
        _ = elevator2.map(_.elevatorId).assertEquals(elevatorId2)
      yield ()
    }
  }

  test("A happy journey: impossible to test in core module, a fiber is required".ignore) {}
