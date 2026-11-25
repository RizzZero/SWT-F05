# CA3 Architecture and Test Review

## Purpose

This note gives the mental model needed to review the CA3 elevator simulator. The system is intentionally small: a service coordinates a domain model with hardware-facing collaborators, while a factory assembles a runnable configuration. The submitted tests exercise some user-visible outcomes, but their structure also gives examples for the course's test-smell and testability discussions. The assignment is a review exercise; this note proposes no code changes.

## Runtime model

`Elevator` stores the current floor, permitted floor bounds, and elevator state. `ElevatorService` coordinates operations. It receives an `Elevator`, `Motor`, `DoorOpener`, `FloorSensor`, and `ElevatorEventPublisher` in its constructor, so those dependencies are visible and replaceable at construction time.

For a normal `moveTo(destination)` call, the service first checks whether the door is open when the supplied door also implements `DoorStatusReader`. It then checks floor bounds, changes the model state to `MOVING`, calls the motor, updates the model floor, publishes an `ARRIVED` event, and sets the model state to `IDLE`. Door operations call the door collaborator, update state, and publish corresponding events. `getCurrentFloor()` reads from `FloorSensor`; it does not return the model's `currentFloor` directly.

The object graph assembled by `ElevatorFactory` is:

```text
properties -> ElevatorFactory -> ElevatorService
                                |-> Elevator
                                |-> Motor implementation
                                |-> SimpleDoor
                                |-> InMemoryFloorSensor
                                |-> ElevatorEventPublisher
```

The properties choose the initial floor and motor type. With `motor.type=legacy`, the factory wraps `LegacyLiftMotor` in `LegacyMotorAdapter`; otherwise it selects `RecordingMotor`. The current configuration selects the legacy path.

## Design patterns and testability choices

### Constructor injection and decomposability

The service's collaborators are passed through its constructor. This is dependency injection: construction makes dependencies visible and allows tests to provide controlled implementations. It supports decomposability and controllability from the Design for Testability slides. The hardware interfaces (`Motor`, `FloorSensor`, and door interfaces) create boundaries between coordination logic and device behavior.

### Factory

`ElevatorFactory` reads configuration and creates the connected object graph. This centralizes assembly and lets callers ask for a service without manually constructing each collaborator. The tradeoff to review is whether the factory's configuration branches and wiring receive enough direct tests.

### Adapter

`LegacyMotorAdapter` implements the project's `Motor` interface and translates `moveTo(floor)` into the legacy API call `selectDestinationAndStart(floor)`. This is a direct Adapter pattern example. `LegacyPanelTest` checks this translation with the supplied legacy motor implementation.

### Observer or event notification

`ElevatorEventPublisher` accepts listeners and publishes string events. `ElevatorServiceTest` subscribes a list collector and checks emitted events. This separates event consumers from service logic, though a string protocol is less explicit and type-safe than a dedicated event type. That is a design observation, not automatically a course-defined smell.

### Polymorphic motor selection

The `Motor` interface gives the factory multiple implementations. The project selects one based on configuration. This is evidence of interface-based substitutability; do not call it the Strategy pattern unless the course's own pattern definition and the intended variation support that label.

## Test suite map

`ElevatorServiceTest` builds a real model and service with concrete, controlled collaborators in `@BeforeEach`, subscribes an event collector, then tests movement, door handling, events, fixture input, and validation. `FactoryTest` checks that the factory can build and use a service. `LegacyPanelTest` checks adapter translation and two recording-motor examples.

The current suite contains eleven `@Test` methods: seven in `ElevatorServiceTest`, one in `FactoryTest`, and three in `LegacyPanelTest`. Tests use concrete recording/fake implementations rather than Mockito mocks; `pom.xml` declares JUnit Jupiter and does not declare Mockito.

## Evidence-based observations

### Conditional verification

`testSensorReadingWithConditionalVerification` has an `if` branch that accepts either floor 1 or floor 0. With the current setup the sensor is initialized to 1, so the expected value is known. The alternative branch weakens the oracle: an unexpected zero can still pass. This matches the Conditional Verification test smell from the Code Smells slides. A clearer test would state the single expected sensor value, but this review does not modify it.

### Eager test and multiple conditions

`testMoveOpenCloseAndNotifyEverything` checks movement, model floor, motor destination, opening, closing, and events in one method. Its name signals broad scope, and a failure can leave several possible causes. This is evidence for the Eager Test / Multiple Test Conditions concepts. The assertions are not literally all one assertion-roulette sequence: their messages and grouping should be discussed precisely rather than overstate the smell.

### Hard-coded data and fixture ambiguity

`testMoveToHardCodedFloor` uses floor 7 directly, which is a small example of hard-coded test data. `testMoveToFloorFromFixtureFile` reads the first CSV data row; the supplied row is floor 1, equal to the setup's initial floor 1. The resulting move does not demonstrate a change of floor, and the fixture's label (`lobby`) is not used. This is a weak scenario and possible irrelevant fixture information. It is not proof that resource-based test data is inherently bad.

### Duplicate scenario

`testRecordingMotorMovesToRequestedFloor` and `testAnotherRecordingMotorScenario` perform the same basic operation and assertion with different constants. This is a small, concrete example of duplicated test code / redundant scenario coverage. The distinction between helpful boundary/value variation and pure duplication can be raised in the oral discussion.

### Indirect or implementation-coupled assertions

`FactoryTest` moves the service, then reads `service.getElevator().getCurrentFloor()`. That asserts the exposed internal model rather than checking the service's sensor-based `getCurrentFloor()` API or another externally meaningful contract. The factory's sensor is initialized to the initial floor, and the supplied `InMemoryFloorSensor` is not visibly connected to movement; therefore, tests that inspect only the model can miss a mismatch between model floor and sensor reading. Whether this is a functional defect depends on the intended sensor contract and hardware behavior, which the supplied assignment does not specify.

### Failure-path state and event behavior

In `ElevatorService.moveTo`, the state becomes `MOVING` before calling the motor. If the motor or event listener throws, no `finally` or recovery transition is visible in this method. This is an untested failure-path question, not a confirmed bug absent a requirement for recovery. Similarly, the bounds and open-door exception tests assert that an exception occurs but do not assert that the model, motor, and event history remain unchanged.

### Runtime type check as a design tradeoff

The service checks `door instanceof DoorStatusReader` before enforcing the closed-door rule. This supports an optional capability interface while also making the service's safety rule depend on a runtime type. Ask whether every usable door should report status, whether a door without that capability is permitted, and what the contract says. Classify as a coupling/testability discussion point rather than a definitive smell without the course definition and domain requirement.

## Coverage and confidence

The assignment centers on code smells and patterns, but coverage criteria help explain what these tests do and do not observe. The suite has examples of both valid movement and selected exceptions, but a line or statement being exercised does not prove that all decision outcomes or interactions are checked. The conditional sensor test illustrates why branch execution alone is insufficient if its oracle permits unexpected outcomes. Branch, condition, and MC/DC reasoning can help design additional review questions; the report does not claim measured coverage. As the slides emphasize, 100% coverage is not proof of correctness or confidence.

## Oral-exam explanation order

1. Explain the assignment as a review, not an implementation task.
2. Sketch service, model, hardware interfaces, factory, adapter, and event publisher.
3. Explain constructor injection, Adapter, Factory, and observer/event notification as design choices.
4. Walk through the strongest test-smell evidence: conditional verification, broad test scope, fixture scenario, and duplicated motor test.
5. Distinguish smells from bugs and design tradeoffs; state what extra requirement or observation would settle uncertain points.
6. Explain why tests and coverage provide evidence, not proof, and why the report leaves code unchanged.

## Review limitations

This is a static review of the supplied source, tests, resources, project description, and course slides. No test result is claimed here. The assignment provides no detailed behavioral contract for sensor synchronization or motor/listener failure recovery, so those points are presented as questions rather than findings of incorrect behavior.
