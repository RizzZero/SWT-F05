# Airplane Simulator

CA2 is a Java 17 Maven project for an airplane flight-lifecycle simulator. The assignment focuses on using Mockito test doubles to examine how the `Aircraft` class coordinates with its components.

For the detailed state model, collaborator responsibilities, failure paths, and test design rationale, see [ARCH.md](ARCH.md).

## Project structure

- `airplane.Aircraft` coordinates the aircraft lifecycle and owns its current `FlightStatus`.
- `airplane.components.Engine` represents engine readiness and operation.
- `airplane.components.FuelSystem` reports fuel availability and consumes fuel.
- `airplane.components.LandingGear` deploys and retracts the landing gear.
- `airplane.components.NavigationSystem` reports whether a route is available.
- `airplane.exceptions.FlightException` represents invalid lifecycle operations and flight failures.
- `airplane.models.FlightStatus` defines `GROUNDED`, `READY`, `IN_FLIGHT`, `LANDED`, and `ABORTED`.

`Aircraft` receives its four collaborators through its constructor. The tests keep `Aircraft` real and replace those collaborators with fresh Mockito mocks so state transitions and interactions can be checked independently.

## Flight lifecycle

1. A new aircraft is `GROUNDED`.
2. `prepareForTakeoff()` checks the engine, fuel, and route. If all are available, the aircraft becomes `READY`.
3. `takeOff()` starts the engine, retracts the landing gear, and changes the state to `IN_FLIGHT`. A runtime failure during takeoff changes the state to `ABORTED` and is wrapped in `FlightException`.
4. `cruise(amount)` requires an in-flight aircraft and a positive fuel amount. It consumes fuel, or performs an emergency landing if the engine has failed. A fuel-consumption failure also causes an emergency landing and is wrapped in `FlightException`.
5. `land()` and `emergencyLand()` deploy the gear, stop the engine, and set the state to `LANDED`.

Invalid lifecycle operations throw `FlightException`. Invalid numeric arguments are intended to throw `IllegalArgumentException`.

## Tests

The JUnit 5 tests are in `src/test/java/airplane/AircraftTest.java`. They cover the initial state, successful preparation, each failed preparation requirement, takeoff success and failure, cruise behavior, landing behavior, and an invalid lifecycle operation. Assertions check both the resulting state and relevant calls to the dependent components.

The test suite demonstrates the Mockito APIs requested by the assignment. Since `consumeFuel` accepts a primitive `double`, its wildcard matcher is the type-specific `anyDouble()`. See [ARCH.md](ARCH.md) for why the generic `any()` form cannot be used safely with this signature and how that maps to the assignment's matcher requirement.

## Build and run

Run the test suite from this directory with:

```text
mvn clean test
```

The Maven configuration targets Java 17 and includes JUnit Jupiter 5.11.4, Mockito Core 5.14.2, and the Mockito JUnit Jupiter integration.

## Verification note

The test suite has not yet been run with Maven. The invalid-number parameterized test includes NaN and positive infinity because the project description says invalid numeric inputs should be rejected. The current `Aircraft.cruise()` guard checks only `fuelAmount <= 0`, so those two values appear to pass that guard. Review this behavior against the assignment's intended numeric-input contract when running the suite.

## Assignment checklist

- The suite contains more than the required six test methods and covers the main `Aircraft` lifecycle and failure paths.
- Before submission, run `mvn clean test` and review any failure, especially the numeric-input case described above.
- Be ready to explain the role of the real system under test, each mock, stubbing, interaction verification, and the `anyDouble()` matcher choice. The assignment includes an oral evaluation of the tests and their behavior.
- The assignment asks teams of two to keep their GitHub repository private until the stated date (1405/08/23), then make it public. Submit the repository URL and the final commit hash using the course's submission method before the deadline.
