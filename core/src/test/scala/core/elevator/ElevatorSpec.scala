package core.elevator

import cats.syntax.applicative.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import core.elevator.Elevator.*
import core.mtl.given
import core.test.*

import TestAppSuite.{*, given}

class ElevatorSpec extends TestAppSuite:
  test("An elevator should return distance from a current floor") {
    runAppT(appEnv, appState) {
      val floorManager = simpleFloorManager[AppT]
      val simulation   = simpleSimulation[AppT]

      for
        elevators <- mkSimpleElevators[AppT](floorManager, simulation)
        elevator   = elevators.headOption.getOrElse(fail("unexpected, no elevator!"))
        _         <- elevator.distance(5).assertEquals(Some(2))
      yield ()
    }
  }

  test("An elevator can not calculate the distance if it is moving to the opposite direction") {
    runAppT(appEnv, appState) {
      val floorManager = simpleFloorManager[AppT]
      val simulation   = simpleSimulation[AppT]

      for
        elevators <- mkSimpleElevators[AppT](floorManager, simulation)
        elevator   = elevators.headOption.getOrElse(fail("unexpected, no elevator!"))
        _         <- elevator.call(0) // set the direction to down
        _         <- elevator.distance(5).assertEquals(None)
      yield ()
    }
  }

  test("An elevator should hold a passenger") {
    val passenger = Passenger.fromFloors(3, 5)
    runAppT(appEnv, appState) {
      val floorManager = simpleFloorManager[AppT]
      val simulation   = simpleSimulation[AppT]

      for
        elevators <- mkSimpleElevators[AppT](floorManager, simulation)
        elevator   = elevators.headOption.getOrElse(fail("unexpected, no elevator!"))
        _         <- elevator.getOn(passenger)
      yield ()
    }.map { (state, _) =>
      state.elevatorStates.headOption.map(_._2).flatMap(_.destinations.headOption) match {
        case Some(Destination.PassengerDestination(p)) => assertEquals(p, passenger)
        case _                                         => fail(show"passenger($passenger) not found")
      }
    }
  }

  test("An elevator should not move if simulation is stopped") {
    runAppT(appEnv, appState) {
      val simulation   = simpleSimulation[AppT]
      val floorManager = simpleFloorManager[AppT]

      for
        elevators <- mkSimpleElevators[AppT](floorManager, simulation)
        elevator   = elevators.headOption.getOrElse(fail("unexpected, no elevator!"))
        _         <- elevator.call(0)
        _         <- elevator.start
      yield ()
    }.map { (state, _) =>
      assertEquals(
        state.elevatorStates.headOption.map(_._2.floor),
        appState.elevatorStates.headOption.map(_._2.floor)
      )
    }
  }

  test("An elevator should move while simulation is running") {
    runAppT(appEnv, appState) {
      val simulation   = simpleSimulation[AppT]
      val floorManager = simpleFloorManager[AppT]

      for
        elevators <- mkSimpleElevators[AppT](floorManager, simulation)
        elevator   = elevators.headOption.getOrElse(fail("unexpected, no elevator!"))
        _         <- elevator.call(0)
        _         <- simulation.start
        _         <- elevator.start
      yield ()
    }.map { (state, _) =>
      assertEquals(state.elevatorStates.headOption.map(_._2.floor), Some(0))
    }
  }
