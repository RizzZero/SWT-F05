# CA2 Audio Guide: Understanding the Assignment, the Airplane Code, and the Tests

## Start here: assignment and mental model

CA2 asks us to test how one part of an airplane simulator works with the components it depends on. The coordinator, `Aircraft`, works with an engine, fuel system, landing gear, and navigation system. The handout asks for at least six tests and for nine listed Mockito methods to appear across the suite. The current class has fifteen test methods, including one parameterized method with five values. Maven has not been run, so no passing result is claimed.

Think of `Aircraft` as a flight manager. It asks components for information, then tells them when to act. It checks engine, fuel, and route before takeoff; starts the engine and retracts gear to fly; and deploys gear and stops the engine to land. Our main question is: when a component reports success, failure, or an exception, does the real coordinator reach the correct state and send the right messages?

This is unit testing of a coordinator, also called the system under test or SUT. Its four collaborators are replaced with Mockito mocks. These tests do not prove the internal correctness of the real component implementations or the whole simulator working end to end. They combine state assertions with interaction checks. The handout permits AI assistance but expects students to explain their tests and concepts in an oral question-and-answer session.

## What “mock” and “stub” mean here

A test double is a substitute object used in a test instead of a real dependency. “Test double” is the broad family name. A dummy is passed around but not meaningfully used. A stub supplies prepared answers. A spy records calls so the test can inspect them. A mock is commonly configured with expected interactions and then verified.

The assignment title says, “Mocks are not stubs.” This is a key idea to be ready to explain. A stub and a mock may look like the same substitute object in code, but the terms describe different purposes in the test. A stub answers a question or supplies a controlled result. A mock lets the test ask afterward whether the system under test sent the expected message, how many times it sent it, and sometimes with what argument. A stub is mainly about setting up the situation; a mock is mainly about checking behavior across a component boundary.

For example, when the test configures `engine.isOperational()` to return true, it is using the engine double as a stub. The test has arranged an indirect input: “the engine reports that it is working.” When the test later checks `verify(engine).start()`, it is using the same Mockito object in the mock role. That check observes an indirect output: “the aircraft told the engine to start.” The object is a Mockito mock technically, but in the story of the test it first provides stubbed behavior and then serves as an interaction observation point.

This distinction helps explain the assignment title without turning it into a rule that one Java object can never do both jobs. In this suite, Mockito creates mocks, stubbing configures their responses, and verification checks selected interactions. A strong oral explanation is: “A stub controls what a dependency returns so I can drive the system under test into a condition. A mock is used to verify what the system did with that dependency. Mockito can provide both roles through one mock object, but the roles are conceptually different.”

There is also a useful caution. If a test only stubs several methods and never checks a result or interaction, it has arranged a scenario but has not shown that the behavior under test is correct. If a test verifies a call without arranging a relevant response, the mock's default answer may not create the scenario the author intended. Good tests make both parts visible: what the double is configured to do, and what outcome or interaction the real `Aircraft` should produce.

## The airplane domain and lifecycle

`Aircraft` owns the status. `FlightStatus` names five states: `GROUNDED`, `READY`, `IN_FLIGHT`, `LANDED`, and `ABORTED`. `FlightException` represents invalid lifecycle actions and selected wrapped failures.

The four collaborators have focused roles. `Engine` reports whether it is operational, starts, stops, and can fail. The real engine begins operational but stopped. `FuelSystem` compares remaining fuel with a takeoff minimum and subtracts consumed fuel; it rejects non-positive amounts and throws if asked for more fuel than remains. `LandingGear` starts deployed and can retract or deploy. `NavigationSystem` stores a destination and reports route availability. The Aircraft tests mock all four, so those real component rules are context, not what this suite proves.

A new aircraft is `GROUNDED`. Preparation is allowed from `GROUNDED` or `LANDED`. It checks engine operation, enough fuel, and route availability in order. The first false result throws `FlightException` and stops later checks; only three true answers lead to `READY`. An exception thrown by a readiness query is not caught by preparation and propagates unchanged.

Takeoff requires `READY`. It calls `engine.start()`, then `landingGear.retract()`, and only then sets `IN_FLIGHT`. A runtime exception from either call sets `ABORTED` and is wrapped in `FlightException`. The suite checks failure at engine start, but not gear retraction failure; if retraction fails, the engine may already be running.

Cruise requires `IN_FLIGHT`, then rejects values where `fuelAmount <= 0`. It checks the engine next. A failed engine check triggers emergency landing and skips fuel consumption. Otherwise, it asks the fuel system to consume the amount. A runtime failure there triggers emergency landing and is wrapped in `FlightException` with the original cause.

The numeric test treats NaN and infinities as invalid, but the handout does not state that contract explicitly. The guard rejects zero, negative values, and negative infinity; it does not reject NaN or positive infinity. With the fuel mock, these two values likely pass through without throwing. With the real FuelSystem, NaN can turn remaining fuel into NaN, while positive infinity can cause an insufficient-fuel exception and a different error path. Call this an inferred finite-positive expectation and a code concern, not a confirmed handout requirement.

Both `land()` and `emergencyLand()` require `IN_FLIGHT` and call the same sequence: deploy gear, stop engine, then set `LANDED`. Failures in these landing calls are not caught, and those paths are not covered. The `ensureStatusIs` guard rejects disallowed operations before collaborators are called. There is no recovery path from `ABORTED` in the current class.

## The JUnit fixture and test structure

The test file is `src/test/java/airplane/AircraftTest.java`. JUnit Jupiter's `@BeforeEach` creates four fresh mocks and one fresh real `Aircraft` before every test. This is the fixture: everything needed to exercise the SUT. Fresh setup makes the tests independent and repeatable.

Helpers named `givenAircraftIsReady` and `givenAircraftIsInFlight` use public operations to reach useful preconditions instead of changing private status directly. This is delegated fixture setup. The tests follow Arrange, Act, Assert: configure the scenario, call the real method, then check the result.

A state assertion such as `assertEquals(IN_FLIGHT, aircraft.getStatus())` checks the aircraft's state. Mockito's `verify` checks messages sent to collaborators. `assertThrows` checks exception type, while `assertSame` can prove that the original exception object remains the cause. These forms of evidence complement one another.

One cruise test calls `reset(fuelSystem)` after the helper has prepared and taken off the aircraft. Reset clears the mock's recorded calls and stubbing, so verification can focus on cruise's fuel call. It is safe because preparation has finished and cruise does not ask `hasEnoughFuel()` again.

## Walking through the current tests

The assignment sets a minimum of six tests, while the current class contains fifteen test methods. To make the minimum requirement meaningful, do not just count names. Understand six representative tests all the way through: what situation each one creates, what the real aircraft does, what evidence the test checks, and which testing concept the test demonstrates. Together, these six examples show a normal path, a rejected path, an unexpected exception, a state transition, fixture management, and emergency handling.

### Test one: all takeoff preparation checks pass

The test is named `prepareForTakeoff_whenAllRequirementsAreMet_becomesReady`. Before the action, it configures the engine's `isOperational` method to return true, the fuel system's `hasEnoughFuel` method to return true, and the navigation system's `isRouteAvailable` method to return true. This is the arrange step. The real `Aircraft` is not mocked; the test calls its real `prepareForTakeoff` method.

The expected result has two parts. First, the aircraft's state must become `READY`. Second, each of the three collaborators must have been queried. These checks combine state-based verification with interaction-based verification. The state proves preparation completed; the verifications show the aircraft reached that decision by consulting all required components.

This is a positive test, sometimes called a happy-path test. It demonstrates `mock`, `when`, `thenReturn`, and `verify`. It also illustrates why stubbing and assertions are separate: the stubs define the situation; the assertions check the result.

### Test two: the engine is not operational, so preparation stops

The test is `prepareForTakeoff_whenEngineIsNotOperational_throwsAndStopsChecking`. It configures the engine check to return false. It does not need to configure the fuel or route mocks because the method should stop at the first failed gate.

The real preparation method is called inside `assertThrows`, which says an exception of type `FlightException` must occur. Afterward, the test checks that the state is still `GROUNDED`, verifies that the engine was queried, and uses `verifyNoInteractions` on the fuel and navigation mocks.

This test is more informative than merely checking that an exception happened. It checks the state was not advanced and proves the short-circuit rule: there is no reason to ask about fuel or route after the engine has already failed. It is a negative test and a test of interaction order. It also shows that false is a normal answer from a dependency, which `Aircraft` converts into its own domain exception.

### Test three: the engine readiness query itself throws

The test is `prepareForTakeoff_whenEngineCheckThrows_propagatesFailureAndRemainsGrounded`. This time the engine does not answer false. Instead, its query throws a prepared `IllegalStateException` object. The test expects that same exception type to escape from `prepareForTakeoff`.

The test then checks two important details. `assertSame` compares the thrown exception's identity with the exception object configured in the mock. This proves that `Aircraft` did not replace or wrap the exception. The state remains `GROUNDED`, and fuel and navigation receive no calls.

Compare this with test two. A false answer causes `Aircraft` to throw `FlightException`. An exception thrown by the collaborator propagates unchanged because preparation does not catch it. These are different failure contracts, and the two tests teach that an unavailable component and a broken component query are not necessarily the same situation.

This test demonstrates Mockito's `thenThrow` stubbing for a non-void method. It also exercises exception assertions and state preservation.

### Test four: takeoff succeeds and the aircraft enters flight

The test is `takeOff_whenAircraftIsReady_startsEngineAndRetractsGear`. Its helper first configures all preparation requirements to pass and calls the real preparation method. That puts the aircraft into `READY` through the normal public operation. The test then explicitly configures the void gear-retraction method to do nothing, calls the real `takeOff` method, and checks that the state is `IN_FLIGHT`.

Finally, it verifies that the engine's `start` method happened exactly once and the landing gear's `retract` method happened exactly once. These calls are the indirect outputs of takeoff. Without those interaction checks, a test could see `IN_FLIGHT` even if the aircraft forgot to coordinate one of its components.

This positive lifecycle test demonstrates `doNothing`, `verify`, and `times(1)`. The explicit no-op setup is not required for Mockito's normal behavior because void methods on a mock do nothing by default. It is included to demonstrate the requested API and to make the expected void-method behavior visible to a reader.

### Test five: cruise consumes the requested amount after clearing setup calls

The test is `cruise_whenAircraftIsInFlight_consumesTheRequestedFuelAmount`. It uses a helper that prepares the aircraft and takes off. Those setup operations have already called methods on the fuel mock, including its takeoff readiness check. The test calls `reset(fuelSystem)` before exercising cruise.

Then it calls `cruise(25.0)`. The expected state remains `IN_FLIGHT`, and the test verifies that `consumeFuel` received the exact value 25.0 exactly once. That exact argument matters: a call with no amount, a different amount, or no call at all would not satisfy the expected interaction.

This test demonstrates `reset` and `times`. Reset clears both the mock's recorded interactions and its stubbing. It is safe here because preparation is already complete; cruise does not call `hasEnoughFuel` again. This is a useful example of why a fixture helper can create interactions that must be considered when designing later verifications.

### Test six: fuel consumption fails, so the aircraft lands and reports the cause

The test is `cruise_whenFuelConsumptionFails_landsAndThrowsFlightException`. Its helper puts the real aircraft into `IN_FLIGHT`. The fuel mock is then configured so that `consumeFuel` throws a prepared `IllegalStateException` for the requested double argument. Because `consumeFuel` returns void, the stubbing uses Mockito's `doThrow` form.

The test calls `cruise(10.0)` and expects a `FlightException`. It checks that the original fuel exception is the new exception's cause, and that the aircraft reaches `LANDED`. It also checks that landing gear deployment and engine stop happened.

This single test illustrates both sides of a mock: it is first configured to inject an indirect input, the failure; afterward, the test verifies the indirect outputs, the landing actions. The argument matcher is `anyDouble`, which means the stubbing applies to a primitive double argument. The test does not merely say “the method failed.” It checks the recovery action and that diagnostic information about the original failure was preserved.

These are six representative tests, not the only six tests in the class. The other nine methods cover the initial grounded state, insufficient fuel, unavailable route, engine-start failure during takeoff, the five invalid-fuel parameter values, engine failure during cruise, ordinary landing, emergency landing, and an attempt to take off from the wrong lifecycle state. The parameterized test is one Java test method but supplies five distinct input cases. The invalid `NaN` and positive-infinity expectations remain an inferred contract and are likely inconsistent with the current guard, as explained earlier.

## Mockito API orientation

Mockito creates and controls the component doubles, then checks their interactions. JUnit runs each test and checks results such as states and exceptions. The detailed explanation of all nine required Mockito methods appears in the final code-specific listening section, where each method is connected to its use in this project.

## Course ideas and completion status

Design for Testability connects constructor injection to controllability, observability, and decomposability: tests choose collaborators, inspect status and calls, and isolate the coordinator. xUnit and JUnit describe setting up a fixture, exercising the SUT, and verifying an expected result. Fixture Management emphasizes fresh, understandable preconditions. Effective Test Design emphasizes clear expected results, independent tests, and both positive and negative cases.

CA1 mainly exercised real controller objects and checked decisions or state. CA2 adds emphasis on replacing dependencies, controlling indirect inputs, and verifying indirect outputs. Both benefit from descriptive names, focused tests, fresh setup, and assertions tied to behavior.

The project has Java 17, Maven, JUnit Jupiter, and Mockito setup. The suite exceeds the six-method minimum and covers the main lifecycle plus selected failures. It has not been run, so do not claim a pass. Further tests could cover gear-retraction failure, landing failures, null constructor arguments, and more invalid lifecycle operations. The current mock-based suite does not test the internals of the real components.

The handout says the assignment is done in teams of two and describes a private GitHub repository or fork, later made public, with the repository URL and final commit hash submitted through the course method. It prints the date 1405/08/23; check current course instructions if that date has passed. This report cannot verify the external submission steps.

## Likely oral questions and short answers

**What is the assignment about?** Testing how the real `Aircraft` coordinator interacts with controlled component doubles.

**What is the SUT?** The real `Aircraft`. Its engine, fuel, gear, and navigation collaborators are mocked.

**What is the difference between a stub and a mock?** A stub supplies controlled answers. A mock is checked for interactions. Mockito can provide both roles through one object.

**Why inject dependencies through the constructor?** It lets a test provide mocks and control the aircraft's environment without changing its logic.

**Why check state and interactions?** State assertions show the result inside `Aircraft`; Mockito verification shows which collaborator messages it sent.

**Why are there separate preparation failure tests?** Each one isolates a gate and checks both the rejection and short-circuit behavior.

**Why `thenThrow` in one test and `doThrow` in another?** The first configures a value-returning method; the second configures a void method.

**Why `anyDouble`?** `consumeFuel` takes primitive `double`; this is the matching wildcard matcher. The source does not make a literal `any()` call.

**Has the suite passed?** It has not been run with Maven. In particular, the `NaN` and positive-infinity expectations likely conflict with the current guard.

**What is not covered?** For example, gear-retraction failure during takeoff, failures during landing, and the internal correctness of the real component classes.

## Final memory summary

Keep this sequence in mind: the assignment teaches test doubles; `Aircraft` is the real unit under test; its four collaborators are mocks; stubbing controls what those mocks report; verification checks what `Aircraft` asks them to do; status assertions check the coordinator's state machine; fresh fixtures make tests repeatable; and the important unresolved observation is that the inferred finite-positive fuel contract is not fully enforced for `NaN` and positive infinity. If you can explain those points and walk through one success test and one failure test, you have the core mental model needed for the assignment discussion.

## Course references consulted

This guide connects the assignment to the course materials on Principles of Effective Test Design, xUnit and JUnit, Fixture Management and Result Verification, Test Doubles, and Design for Testability. The assignment-specific requirements and examples come from the CA2 handout and the project's current Java source and test files.

## Final listening recap: the handout's priorities

CA2 is designed to teach test doubles, not to add airplane features. The handout asks you to control components, test the real coordinator, and explain how each test works. Its core learning contrast is between indirect inputs, such as an engine answer or injected exception, and indirect outputs, such as starting the engine or deploying gear.

The title, “Mocks are not stubs,” is about roles. A stub supplies an answer; a mock is used to verify expected messages. Mockito can provide both roles through one object: `when` and `thenReturn` set up responses, while `verify` and `times` inspect interactions. The suite also checks state and exceptions with JUnit.

At least six tests are required, with each listed Mockito concept used across the suite. The current suite has fifteen test methods. Do not stop at a count: for each scenario, explain its starting state, mock setup, real Aircraft action, and expected state, exception, or collaborator call. The larger assessment emphasis is the oral explanation of testing concepts and how the tests behave; AI assistance does not remove the need to understand them.

The handout also specifies a team of two and the GitHub visibility and submission process described above. Be precise about sources: the handout gives learning and submission requirements; the Java implementation defines current behavior; some test expectations, including finite-positive cruise amounts, are inferred and should be labeled as such.

## Extra listening section: the nine Mockito methods in our actual code

This final section applies the short Mockito explanations to the exact classes and tests in CA2. The examples earlier used generic names such as `Airplane` and `getSpeed`. Those were teaching examples. Our actual system under test is `Aircraft`, and our actual collaborators are `Engine`, `FuelSystem`, `LandingGear`, and `NavigationSystem`.

Before listening to each method, keep the test shape in mind. JUnit runs the test and checks outcomes. Mockito creates substitute collaborators, controls their answers, and records their calls. In the CA2 tests, the `Aircraft` object stays real throughout. The mocks take the place of the four components around it.

### `mock()`: create each controlled collaborator

In `AircraftTest.setUp`, the test calls `mock(Engine.class)`, `mock(FuelSystem.class)`, `mock(LandingGear.class)`, and `mock(NavigationSystem.class)`. These are four separate substitute objects. They have the expected Java types, so the `Aircraft` constructor can accept them, but their real component logic is not being tested in this suite.

The test then creates a real aircraft with `new Aircraft(engine, fuelSystem, landingGear, navigationSystem)`. This matters: we are asking whether the real coordinator behaves correctly when its surroundings give controlled answers. We are not asking Mockito to pretend to be the aircraft.

Mockito mocks have default answers for unstubbed calls. In particular, an unstubbed boolean query returns false. That is why the tests explicitly configure true for the readiness checks when they need a successful path. Fresh mocks are created before every test by `@BeforeEach`, so an engine configured to fail in one test cannot accidentally remain failed in another.

### `reset()`: clear the fuel mock after building the flight fixture

In `cruise_whenAircraftIsInFlight_consumesTheRequestedFuelAmount`, helper methods first prepare the aircraft and take off. During preparation, the fuel mock receives a call to `hasEnoughFuel()`. The test wants to focus its interaction check on the later call to `consumeFuel(25.0)`, so it calls `reset(fuelSystem)` after the setup flight has already been created.

Reset makes the fuel mock behave like a newly created mock: it clears both recorded interactions and configured behavior. This does not reset the real aircraft's state. The aircraft remains in flight, and the engine mock's setup is not affected, because only the fuel mock is reset. Cruise can then be exercised and its fuel-consumption call checked without the earlier preparation call confusing the interaction history.

In a general test suite, creating a fresh fixture is usually clearer than reusing mocks and resetting them. Here, each test already has fresh mocks, and this one focused reset is used to demonstrate the required method and isolate the relevant interaction.

### `when()`: choose a non-void call whose answer we want to control

The helper `givenTakeoffRequirementsAreMet()` contains calls such as `when(engine.isOperational())`. The method inside `when` identifies which method on the mock is being configured. Its partner, `thenReturn`, supplies the answer.

The `when(...).thenReturn(...)` form is used for methods that return a value: `engine.isOperational()`, `fuelSystem.hasEnoughFuel()`, and `navigationSystem.isRouteAvailable()`. It gives the test a simple way to set up conditions without changing the real component state. For a failed preparation test, the test configures one check to return false. For the successful path, it configures all three to return true.

The condition being configured is not a condition in the program's business logic; it is the method call whose response the test wants Mockito to control. The real `Aircraft` still decides how to react to that response.

### `thenReturn()`: supply the scenario's answer

In the successful preparation case, the test stubs all three readiness checks to return true. In the engine-rejection case, it stubs `engine.isOperational()` to return false. In the fuel-rejection case, it returns true for the engine and false for `fuelSystem.hasEnoughFuel()`.

These different return values create different indirect inputs. They let the tests drive the real aircraft down specific branches. The expected result is then checked separately: for example, all true answers should lead to `READY`, while a false answer should cause `FlightException` and leave the status unchanged.

Mockito supplies the configured value; it does not execute the real method body of the mocked component. Therefore, a stubbed `hasEnoughFuel()` returning true does not prove that the real fuel calculations are correct. It proves how `Aircraft` responds when its fuel collaborator reports success.

### `verify()`: observe what the real aircraft asked a collaborator to do

After the action, the tests use calls such as `verify(engine).start()`, `verify(landingGear).retract()`, and `verify(fuelSystem).consumeFuel(25.0)`. Mockito has recorded the calls made to its mock. `verify` checks that the expected call appears in that record.

For successful preparation, the test verifies that the engine, fuel system, and navigation system were queried. For successful takeoff, it verifies engine start and gear retraction. For emergency landing, it verifies gear deployment and engine stop. In failure cases, it can verify that some call did not happen.

This is behavior or interaction verification. It checks messages crossing from the real `Aircraft` to its dependencies. It is different from a JUnit state assertion. For example, `assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus())` checks the aircraft's state; `verify(landingGear).retract()` checks a call to the gear collaborator. A robust test uses the evidence relevant to its behavior.

### `times()`: state how many times a call should occur

In the successful takeoff test, the source uses `verify(engine, times(1)).start()` and `verify(landingGear, times(1)).retract()`. The successful cruise test uses `times(1)` when it checks fuel consumption. These assertions say that the call occurred exactly once.

The number can matter. If takeoff started the engine twice, or cruise consumed the requested fuel twice, the final state might still look plausible while the interaction was wrong. `times(2)` would mean exactly two calls; `times(0)` means no calls. The tests also use `never()` for the no-call case, which is a readable Mockito verification equivalent to requiring zero calls.

Mockito's `verify(mock).method()` already means one call by default. Writing `times(1)` is therefore explicit and is useful here because the assignment asks for `times()` to appear in the suite.

### `any()`: the generic idea, and the primitive matcher our code needs

The handout names `any()` as a general argument matcher. The idea is to say, “I care that this method was called, but for this check I do not need to name one exact argument.” For example, a method that accepted a `String` or another object could be checked with a compatible generic matcher.

Our collaborator method is `consumeFuel(double amount)`. Java's `double` is a primitive, so the actual test uses `anyDouble()`. It appears in the invalid-amount test's `verify(fuelSystem, never()).consumeFuel(anyDouble())`, and in the fuel-failure setup `doThrow(failure).when(fuelSystem).consumeFuel(anyDouble())`.

The first use means: verify that fuel consumption never happened, whatever double argument might have been supplied. The second means: configure the fuel mock to throw when `consumeFuel` is called with a double. The exact requested amount is not important to that failure setup, so the matcher is appropriate.

Our source does not literally call `any()`. It uses the type-specific `anyDouble()` version because that matches the provided method signature. Do not tell the examiner that a literal `any()` appears in the code. Say that the suite demonstrates wildcard argument matching with the primitive-specific matcher. This is the safe, meaningful choice for a `double` argument; a generic reference matcher is not meant to stand in for this primitive parameter.

### `thenThrow()`: make a value-returning collaborator fail

The test `prepareForTakeoff_whenEngineCheckThrows_propagatesFailureAndRemainsGrounded` creates an `IllegalStateException` and configures `when(engine.isOperational()).thenThrow(failure)`. The call to `isOperational()` normally returns a boolean, but this test makes the query itself fail instead.

The real aircraft calls that query during preparation. Because `prepareForTakeoff` does not catch this exception, the exception propagates. The test uses `assertThrows` to check its type and `assertSame` to confirm the exact original exception object is still present. It also checks that the state remains `GROUNDED` and that later readiness checks were not made.

The suite also uses `doThrow()` for void methods such as `engine.start()` and `fuelSystem.consumeFuel(...)`. That is an additional Mockito API beyond the nine listed in the handout. The distinction is based on method signatures: `thenThrow` fits the `when` stubbing chain for a value-returning call; `doThrow` is the form used to configure a void method to throw.

### `doNothing()`: explicitly configure a successful void call

In `takeOff_whenAircraftIsReady_startsEngineAndRetractsGear`, the test includes `doNothing().when(landingGear).retract()`. It then calls the real `aircraft.takeOff()` and verifies that retraction happened once.

The configured gear mock has no real gear behavior and does not move anything. The purpose of this setup is simply to say that the mock should accept the void call without throwing. Mockito already makes void methods on a normal mock do nothing by default, so this line is redundant for the test's runtime behavior. It is present to demonstrate the requested API clearly, and `verify` still records and checks that the call occurred.

This illustrates two separate ideas: `doNothing` configures the void method's response; `verify` checks whether the SUT invoked it. The mock does not need to perform a physical gear movement for the coordinator test to establish that the correct command was sent.

### Put the methods together

In successful takeoff, `mock()` creates collaborators; `when()` and `thenReturn(true)` set readiness answers; `doNothing()` configures the void gear call; the real `takeOff()` runs; `verify()` with `times(1)` checks engine start and gear retraction; and a JUnit assertion checks `IN_FLIGHT`. For cruise success, setup reaches flight, `reset()` clears fuel-mock setup history, and verification checks the exact amount. For fuel failure, `anyDouble()` matches the primitive argument and `doThrow()` injects the exception. Mockito controls and observes dependencies; JUnit checks outcomes.

## Final technical lesson: read the design and tests like a tester

This closing lesson connects the simulator code to the testing subject. The goal is to see why the tests are designed this way, what they establish, and where their evidence stops.

### The coordinator and its boundary

Think of `Aircraft` as the decision maker. It owns the flight status and calls the engine, fuel system, landing gear, and navigation system when needed. Constructor injection gives tests control over those dependencies. That improves controllability because a test can decide what each collaborator reports; decomposability because the coordinator can be tested alone; and observability because the test can inspect status and recorded calls.

In `AircraftTest`, only `Aircraft` is real. The other four objects are Mockito mocks. So these are unit tests of a coordinator, not an integration test of all components and not an end-to-end simulator test. A verification that `consumeFuel(25.0)` was called does not prove that real fuel arithmetic is correct. The real `FuelSystem` would need separate tests.

### Status is a small state machine

The aircraft starts `GROUNDED`. Successful preparation leads to `READY`. Successful takeoff leads to `IN_FLIGHT`. Ordinary or emergency landing leads to `LANDED`. A runtime failure during takeoff leads to `ABORTED`. The guard `ensureStatusIs` checks whether an operation is allowed before the aircraft calls collaborators.

This explains the invalid lifecycle test. Taking off from `GROUNDED` must throw before any collaborator is called, and the state must remain grounded. Preparation is allowed from grounded or landed. Takeoff is allowed only from ready. Cruising and landing are allowed only in flight. These rules are externally visible behavior and are natural unit-test targets.

### Order reveals intent

Preparation checks engine, fuel, and route in sequence. A failed early check stops later checks. That is why the tests verify not only the exception but also that fuel or navigation was not contacted after an earlier failure.

Takeoff starts the engine, retracts gear, then changes status. If engine start fails, the gear call does not happen. The code also catches a failure from gear retraction, but that exact path is not tested. Since the engine could already be running at that point, a useful design question is whether the catch block should stop it as compensation.

Landing deploys the gear, stops the engine, and then sets `LANDED`. The current implementation does not catch failures from deployment or stop. If either throws, status may not become landed. Those paths are not covered by the current suite. Naming such a gap is better than claiming complete coverage.

### The fixture and evidence

`@BeforeEach` creates fresh mocks and a fresh aircraft. Helpers put the aircraft into `READY` or `IN_FLIGHT` through public methods. This follows the xUnit rhythm: arrange a fixture, act on the SUT, and verify the expected result. Fresh fixtures make tests independent and repeatable.

Use both kinds of evidence when they answer different questions. `assertEquals` on status checks state. `assertThrows` checks exception type. `assertSame` on a cause checks that the exact original exception was preserved. Mockito `verify` checks collaborator calls. `times(1)` checks one call; `never()` checks zero; `verifyNoInteractions` checks that the selected mocks were untouched.

Do not verify every call just because Mockito can record it. Verify the messages that are part of the behavior under test. The cruise success case resets only the fuel mock after setup so its verification focuses on cruise's fuel-consumption message, not the earlier takeoff readiness check.

### Expected behavior and the numeric question

The cruise method first checks lifecycle state, then rejects values satisfying `fuelAmount <= 0`, then checks the engine, and finally consumes fuel. If the engine is not operational it emergency-lands. If fuel consumption throws, it emergency-lands and wraps the cause.

The test also treats `NaN` and infinities as invalid, based on a reasonable interpretation that fuel quantities should be finite and positive. The assignment handout does not explicitly state this numeric contract. The current guard rejects zero, negative numbers, and negative infinity. It does not reject `NaN` or positive infinity because neither is less than or equal to zero. In the mock-based test those values probably pass through without throwing, so the test expectations likely fail. With the real `FuelSystem`, `NaN` can contaminate the remaining fuel value, while positive infinity can trigger insufficient-fuel behavior and a different exception path. Call this an inferred contract and implementation concern, not a confirmed requirement from the handout. Maven has not been run, so do not report a pass result.

### Your oral explanation

A compact answer that connects the whole design is: “The real `Aircraft` is the system under test. Constructor injection lets the test replace its four collaborators with fresh Mockito mocks. Stubbing controls indirect inputs such as readiness answers and exceptions. Verification checks indirect outputs such as engine start and gear movement. JUnit assertions check status and exception outcomes. The suite covers success, rejection, lifecycle guards, and selected recovery paths. It does not test the real components together. The numeric `NaN` and positive-infinity cases express an inferred finite-positive contract, and the current guard may not satisfy them.”

Then choose one test and explain it in order: starting condition, configured mock response, real aircraft call, and expected evidence. For example, the fuel-failure test starts in flight, makes the void `consumeFuel` call throw, calls cruise, and checks emergency landing plus a `FlightException` whose cause is the original fuel error. That answer shows knowledge of both Mockito and the domain behavior.

## Course references consulted

This guide connects the assignment to Principles of Effective Test Design, xUnit and JUnit, Fixture Management and Result Verification, Test Doubles, and Design for Testability. The assignment-specific requirements come from the CA2 handout; behavior descriptions come from the project's Java source and test files.
