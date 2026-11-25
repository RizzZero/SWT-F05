# CA3 Elevator Simulator Review

## 1. Assignment, scope, and method

This assignment asks the team to review a supplied elevator simulator for course-taught code smells, behavioral smells, design patterns, and testability. The title mentions refactoring, but the project README and assignment description frame this submission as a review: the code and tests are already supplied, and no fixes are expected. The team should be able to explain the concepts and the test behavior during an oral question session.

This report treats the codebase as the subject of analysis. It does not change production code or tests, and it does not claim a test run. Findings are limited to categories and terminology in the supplied course slides. A smell is a warning sign, not a diagnosis of incorrect behavior. Where requirements are missing, this report states a question rather than asserting a defect.

The project is a Java 17 Maven application using JUnit Jupiter. The test source currently contains eleven `@Test` methods across `ElevatorServiceTest`, `FactoryTest`, and `LegacyPanelTest`. Test collaborators are concrete recording or in-memory implementations; the project does not use Mockito in its current test suite.

## 2. System model

The central `ElevatorService` coordinates an `Elevator` model with a motor, door, floor sensor, and event publisher. Its constructor accepts these collaborators directly (`ElevatorService.java`, lines 17-24), which makes the dependencies visible and replaceable. The model stores a current floor, min/max bounds, and state (`Elevator.java`, lines 4-14).

For movement, the service checks an optional door-status capability, validates the destination, sets state to `MOVING`, calls the motor, changes the model floor, publishes an arrival event, and sets state to `IDLE` (`ElevatorService.java`, lines 26-35). The sensor-facing method reads from `FloorSensor` (`ElevatorService.java`, lines 50-52), whereas the model has a separate current-floor value.

`ElevatorFactory` loads `elevator.properties`, creates the model and collaborators, chooses either the legacy motor adapter or recording motor, and returns a service (`ElevatorFactory.java`, lines 15-24). The current properties select initial floor 1 and motor type `legacy`. The factory initializes the in-memory sensor at the initial floor; the supplied service movement code does not update that sensor.

## 3. Positive patterns and testability choices

### Dependency injection and decomposition

Constructor injection supplies explicit dependencies to `ElevatorService`. This supports decomposition and controllability: tests can choose a recording motor, simple door, in-memory sensor, and event collector without constructing real hardware. The narrow interfaces in the hardware package also keep the service from depending directly on concrete motor or sensor implementations.

### Factory

`ElevatorFactory` centralizes configuration and object assembly. It gives the application one place to select implementations from properties. This is useful wiring structure; a review can still ask whether each configuration branch has a test.

### Adapter

`LegacyMotorAdapter` implements the project's `Motor` abstraction and translates `moveTo(floor)` into the legacy method `selectDestinationAndStart(floor)` (`LegacyMotorAdapter.java`, lines 3-12). `LegacyPanelTest.testLegacyMotorAdapterMovesToRequestedFloor` checks that translation. This is a direct Adapter pattern, included to make an incompatible API usable through the project's expected interface.

### Observer-style event notification

`ElevatorEventPublisher` lets listeners subscribe and receives events from the service. The service publishes arrival and door events, while `ElevatorServiceTest` captures them. This resembles the Observer pattern and helps separate event consumers. Events are represented as strings, so event names and payload structure are not enforced by a type system; treat that as a design tradeoff unless the course definition classifies it as a smell.

### Interface-based implementation choice

Both the legacy adapter and recording motor implement `Motor`, and the factory selects one based on configuration. This demonstrates polymorphism and substitutability. Do not label it the Strategy pattern unless the course slides define the pattern in a way that fits this exact role.

## 4. Main test findings

The strongest findings are the tests whose behavior directly weakens clarity or the assertion oracle. The following are review observations, not edits.

### Finding 1: Conditional Verification in the sensor test

**Evidence:** `ElevatorServiceTest.testSensorReadingWithConditionalVerification`, lines 77-85, branches on the observed floor and accepts either 1 or 0. Setup initializes the sensor to 1 (line 32).

**Course concept:** Conditional Verification, under Conditional Test Logic in the Code Smells slides.

**Why it matters:** The test already knows the arranged input and expected reading. An unexpected reading of 0 can still pass through the alternate branch, so the test does not state one clear expected result. The test can therefore conceal a regression instead of detecting it.

**Review direction:** State one expected sensor value for this arrangement. If multiple legitimate outcomes exist, make them separate named scenarios with explicit setup and expectations.

### Finding 2: Broad test with several behaviors

**Evidence:** `ElevatorServiceTest.testMoveOpenCloseAndNotifyEverything`, lines 39-56, moves the elevator, checks the model and motor, opens and closes the door, and checks event notifications in one method.

**Course concept:** Eager Test and Multiple Test Conditions. The method exercises several responsibilities, so diagnosis requires locating which phase failed.

**Why it matters:** A failure has a larger search space and the method is harder to use as a precise explanation of one behavior. Its assertions are understandable, so do not automatically call it Assertion Roulette; explain the specific broad-scope concern.

**Review direction:** Prefer focused scenarios organized around one behavior or one closely related contract, while retaining a separate end-to-end coordination test if that broad integration behavior is valuable.

### Finding 3: Fixture-driven test does not change the state

**Evidence:** `testMoveToFloorFromFixtureFile`, lines 58-69, reads the first CSV floor and moves there. The supplied first row is floor 1 (`src/test/resources/floors.csv`), which equals the setup's initial floor 1. The test asserts that the floor is 1 afterward. The label field is not used.

**Course concepts:** Hardcoded/irrelevant data and an obscure or weak test scenario. The CSV itself is an external fixture, but using a fixture is not inherently a smell.

**Why it matters:** The test name suggests it checks movement based on fixture data, yet its selected row does not demonstrate a floor change. A test can pass without proving that the fixture-derived destination is actually applied as a changed destination.

**Review direction:** Choose a fixture row different from the starting floor and assert the resulting contract. Keep only fixture fields that contribute to the behavior under test.

### Finding 4: Redundant recording-motor scenario

**Evidence:** `LegacyPanelTest.testRecordingMotorMovesToRequestedFloor` (lines 20-25) and `testAnotherRecordingMotorScenario` (lines 27-32) each create a `RecordingMotor`, call `moveTo` with a constant, and assert the same stored destination.

**Course concept:** Test Code Duplication / redundant scenario coverage.

**Why it matters:** The second test adds little distinct behavioral evidence and creates another place to maintain the same setup and assertion. Different values alone do not always represent meaningfully different equivalence classes.

**Review direction:** Keep separate cases only when they represent a distinct boundary, rule, or contract; otherwise consolidate or remove redundant repetition in a future authorized revision.

### Finding 5: Exception tests omit non-occurrence assertions

**Evidence:** `testRejectsOutOfRangeFloor`, lines 99-102, asserts an `IllegalArgumentException`. `testDoorMustBeClosedBeforeMoving`, lines 104-108, opens the door and asserts an `IllegalStateException`. Neither test asserts that rejected movement leaves state unchanged, avoids motor invocation, and publishes no arrival event.

**Course concept:** Test coverage and oracle completeness; this is primarily a missing assertion/interaction check rather than a named smell by itself.

**Why it matters:** The exception assertion verifies one visible outcome, but it does not establish that the rejected operation had no side effects. These tests may not catch a future validation-order regression.

**Review direction:** Identify the contract for rejected movement and check both the exception and relevant unchanged state/interactions. Do not add assertions merely for more coverage; choose them from the behavior contract.

### Finding 6: Factory test observes an internal model value

**Evidence:** `FactoryTest.factoryBuildsAWorkingElevatorService`, lines 9-14, moves to floor 6 and asserts `service.getElevator().getCurrentFloor()`.

**Course concept:** Indirect Testing / test observability discussion.

**Why it matters:** The check reaches through the service to inspect its internal model. The service also exposes `getCurrentFloor()`, which reads from the sensor. Since the factory initializes the sensor at the initial floor and the visible movement code does not synchronize it, the current test can pass while the service's sensor-facing reading remains at the old floor.

**Qualification:** Whether this is a defect depends on whether the sensor is expected to change independently as hardware reports a physical movement. The supplied code does not establish that contract. The evidence supports a testability and contract question, not a definitive functional bug.

**Review direction:** Clarify the sensor contract and test the externally meaningful reading using a controlled sensor that reflects movement, or assert explicitly that model and sensor have separate responsibilities.

## 5. Production design questions and untested behavior

### Failure during movement

`ElevatorService.moveTo` sets the state to `MOVING` before invoking the motor and publishing the event (`ElevatorService.java`, lines 31-35). If one of those collaborators throws, the method as written has no visible recovery transition. No supplied requirement states what state should follow a hardware or listener failure, and no test demonstrates the intended behavior. Present this as a failure-path question, not as a confirmed bug.

### Optional door status

The service uses `instanceof DoorStatusReader` to determine whether it can check that a door is closed (`ElevatorService.java`, line 27). This may be a reasonable optional-capability interface, but the safety behavior then depends on the concrete collaborator implementing that second interface. Ask whether every door implementation must report status and what the service should do if it cannot know whether the door is open. This is a design-for-testability and contract discussion, not automatically a smell.

### Model and sensor consistency

The domain model and sensor provide separate floor values. The service updates the model after motor movement and reads the sensor for `getCurrentFloor()`. The factory's in-memory sensor begins at the configured floor but has no visible link to the motor or model. A hardware sensor might update independently in a real system; the in-memory implementation will not unless explicitly updated. This behavior should be clarified and tested according to the intended boundary.

### Test setup and hard-coded values

`ElevatorServiceTest.setUp`, lines 27-36, creates a general fixture used by all tests, with fixed initial floor and bounds. The shared fixture is currently small and readable, so it is not automatically a General Fixture smell. Still, fixed values such as 7, 11, and initial floor 1 should be tied to scenario intent. The out-of-range case uses 11 while factory max is 10, a meaningful boundary example.

## 6. Coverage reasoning

The suite includes ordinary movement, a few door operations, event collection, an out-of-range request, and a request while the door is open. It does not visibly demonstrate the alternate motor configuration, sensor changes after movement, invalid factory configuration, listener failure, motor failure, or all boundary values. These are candidate untested requirements only when the assignment's expected behavior demands them.

Coverage criteria help ask whether the suite reaches statements, branches, conditions, or combinations of conditions. They do not tell us whether the assertions are strong. In the sensor test, both branches could be exercised over different arrangements and still allow an incorrect reading. MC/DC or branch coverage cannot repair a permissive oracle. Conversely, an uncovered path is not necessarily a defect if it is infeasible or outside scope. The course slides explicitly caution that a high or complete coverage percentage does not establish correctness or confidence.

## 7. Behavioral and project smell screening

The Behavioral Smells slides also cover Assertion Roulette, erratic tests, fragile tests, frequent debugging, manual intervention, and slow tests. The static evidence reviewed here does not justify asserting that the suite is flaky, slow, or requires manual intervention. Those properties require execution history, timing, environment behavior, or human steps that are not present in the supplied files. The broad movement test has several assertions, but its checks are still understandable; calling it Assertion Roulette would overstate the evidence. The conditional sensor oracle and redundant motor scenarios are stronger course-grounded classifications.

The Project Smells slides discuss project-level signals such as test defects, maintenance cost, escaped defects, lost tests, and risk metrics. The repository snapshot contains no project history, defect reports, CI results, or coverage measurements. Therefore this review does not claim a project smell based on trend or process data. The eleven existing test methods and the identified assertion gaps can inform a future project-health discussion, but they do not establish escaped defects or test loss.

## 8. Oral question preparation

**What is the purpose of this CA3?** Review supplied code and tests for course-taught smells and patterns, then explain the reasoning. It is not a request to rewrite the project.

**What is the strongest test smell?** Conditional Verification in `testSensorReadingWithConditionalVerification`: the known arrangement expects 1, but a value of 0 is accepted too.

**Why is the CSV test weak?** Its first data row selects floor 1, the initial floor, so the move does not show a changed floor; the fixture's label is also unused.

**Name two design patterns with direct evidence.** The Factory centralizes object construction and configuration. `LegacyMotorAdapter` maps the legacy motor API to `Motor`. The event publisher/listener structure is Observer-style notification.

**Is a smell a bug?** No. A smell is evidence that code may be harder to understand, test, or maintain. A bug requires a violated behavioral requirement or demonstrated incorrect result.

**Why not call the sensor behavior a definite bug?** The in-memory sensor does not visibly track service movement, but a real sensor may update independently. The intended contract is not specified.

**Does coverage prove correctness?** No. Coverage says which structural elements were exercised under selected tests. It does not prove assertions are correct, requirements are complete, or all defects are absent.

**What does dependency injection buy here?** The service receives its collaborators explicitly, making wiring visible and allowing controlled implementations in tests.

## 9. Conclusion

The simulator offers useful positive examples of constructor injection, interfaces, factory-based assembly, adapter translation, and event notification. The tests also provide clear discussion points: conditional verification weakens the sensor oracle; one test combines several behaviors; the CSV-selected floor does not differ from the initial floor; two recording-motor tests are near duplicates; and exception tests do not check the absence of side effects. Additional design questions concern state after collaborator failures, optional door-status capability, and the relationship between the model and sensor.

The report intentionally distinguishes direct evidence from interpretation. The team should be able to explain what each test arranges, what it exercises, what it asserts, and what it leaves unknown. The Java files remain unchanged, consistent with the assignment's review-only scope.

## Sources

- CA3 assignment description: `SWT-05-CA3.pdf`.
- CA3 project README and the supplied Java production and test sources.
- Course slides: 12 Design for Testability; 13 Code Smells; 14 Behavioral Smells; 15 Project Smells; 16 Test Coverage Criteria.
