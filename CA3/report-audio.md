# CA3 Audio Study Guide

## Begin with the assignment

This assignment is a review of a small elevator simulator. The code and the JUnit tests are already provided. Our job is to recognize and explain examples of code smells, test smells, design patterns, and testability choices using the course slides. The assignment does not ask us to repair the project. That distinction matters: we should be able to recommend a direction, but our deliverable is an evidence-based review.

The questions in the oral discussion will likely ask us to explain what a test does, why a structure may be a smell, which course concept applies, and what the consequence is. We should also be ready to say when something is only a concern or an open question. A smell is a warning sign. It is not, by itself, proof of a software defect.

## Picture the program

Think of the elevator service as the coordinator. It receives an elevator model, a motor, a door, a floor sensor, and an event publisher through its constructor. That arrangement is called dependency injection. The service does not create all its own dependencies; another part of the program provides them.

When a move is requested, the service checks whether a door-status reader is available and whether the door is open. It checks the requested floor against the allowed range. It changes the elevator state to moving, tells the motor to move, updates the model's floor, publishes an arrival event, and returns the model state to idle.

The model and the sensor are separate. The model remembers a floor. The service's `getCurrentFloor` method asks the sensor for a reading. This distinction becomes important when we study the factory test: a test can inspect the model's floor and pass even when the sensor reading has not changed.

The factory reads configuration and assembles the objects. In the supplied configuration, the legacy motor is selected. Because its method names do not match the application's `Motor` interface, a `LegacyMotorAdapter` translates the application call into the legacy motor's call. The service also publishes events that listeners can receive, which is an Observer-style arrangement.

## What the tests cover

There are eleven test methods at present. Seven are in `ElevatorServiceTest`, one is in `FactoryTest`, and three are in `LegacyPanelTest`. They use JUnit and concrete in-memory or recording collaborators. This project does not use Mockito in the tests.

The service tests cover a movement and door sequence, a destination read from a CSV fixture, a hard-coded destination, a sensor reading, several destinations in one method, an out-of-range request, and a movement request while the door is open. The factory test creates a service and moves it. The legacy panel tests check the adapter and the recording motor.

When explaining a test, use four simple questions. What does it arrange? What action does it perform? What result does it assert? What important result does it leave unchecked? This keeps the explanation precise and helps us avoid saying that a test proves more than it actually proves.

## The clearest test smell

The sensor test has an `if` statement. It reads the sensor, then says: if the value is one, assert one; otherwise, assert zero. But the test setup already starts the sensor at one. The expected result is known. Accepting zero means a wrong reading can still pass.

The course calls this Conditional Verification, a form of Conditional Test Logic. The problem is not simply that an `if` appears in test code. The problem is that the test accepts multiple outcomes without the scenario justifying those outcomes. A clearer test would arrange the sensor at one and state directly that the expected reading is one.

If asked how coverage relates to this, explain that executing both branches would not make the test's oracle stronger. Coverage tells us that code ran. It does not decide whether the expected result is correct.

## A test with too many jobs

The test named `testMoveOpenCloseAndNotifyEverything` checks movement, the model floor, the motor destination, opening the door, closing the door, and event notifications. That is a lot of behavior in one method.

This connects to Eager Test and Multiple Test Conditions from the Code Smells slides. If a failure appears, we need to work through several operations to find its cause. We should describe that broad scope accurately. The assertions are not necessarily confusing enough to call this Assertion Roulette. Good review work uses the narrowest supported classification.

## The fixture and its hidden weakness

The fixture file contains a first row for floor one, labeled lobby. The test setup also starts the elevator on floor one. The test reads that row, asks the service to move to floor one, and checks that the model is still on floor one. So the test named as a fixture movement test does not actually show a floor change. The text label from the fixture is not used either.

This is a weak scenario and an example for discussing hard-coded or irrelevant data. It does not mean that using a CSV fixture is automatically a smell. A fixture is useful when its contents contribute to a meaningful scenario. Here, choosing a destination different from the starting floor would make the movement evidence clearer.

## Redundant and incomplete tests

Two tests for the recording motor create a motor, move to a constant floor, and assert that same floor. One uses floor three and the other floor five. Unless those values represent different rules or boundaries, the second test adds little new information. This is a small example of test-code duplication or redundant scenario coverage.

The out-of-range and open-door tests assert that an exception is thrown. That is useful, but they do not also assert that the elevator state stayed unchanged, the motor was not called, and no arrival event was published. We should describe this as an incomplete oracle or missing side-effect assertion, not force it into a named smell category without evidence.

## Patterns and design for testability

Constructor injection makes collaborators visible and replaceable. The `Motor` and `FloorSensor` interfaces separate service logic from implementations. The factory centralizes object assembly. The adapter translates between two incompatible motor APIs. The event publisher and listener provide Observer-style notification.

These choices support decomposition and controllability, two ideas from Design for Testability. A good review mentions these positive structures as well as the weaknesses. The code is not simply good or bad; it contains tradeoffs.

One tradeoff is that the service checks whether the door object also implements `DoorStatusReader`. This lets door status be an optional capability, but the service can only enforce the open-door rule if that capability exists. We do not know from the assignment whether every door must support status. So we should ask about the intended contract rather than immediately call it a defect.

Another tradeoff is the model versus sensor distinction. The factory initializes the sensor at the initial floor, while movement updates the model. In a physical system, a sensor may report movement independently. The in-memory sensor does not visibly do that. Since the intended contract is not specified, this is an important design and testing question, not a confirmed bug.

## Coverage and project quality

The coverage slides discuss statements, branches, paths, conditions, and MC/DC. These are different ways to ask which parts of logic the tests exercise. They are useful for finding gaps, but none of them proves that requirements are complete or assertions are correct.

For example, the suite has a successful movement and selected exception cases, but the supplied tests do not visibly demonstrate the alternate motor configuration, sensor updates after movement, motor failures, or event-listener failures. These are candidate test gaps only if the expected behavior requires them.

The slides also caution that one hundred percent coverage is not one hundred percent confidence. A test could execute every line and still accept the wrong answer. The conditional sensor test is a concrete example of that distinction.

## What we cannot claim from this snapshot

The course also teaches behavioral and project-level smells. We should be careful with those labels. The files alone do not tell us whether the suite is slow, flaky, or requires manual intervention. Those judgments need timing, repeated execution, environment history, or a known manual step. The broad movement test has several understandable assertions, so calling it Assertion Roulette would be stronger than the evidence supports.

Project smells such as escaped defects, lost tests, or rising maintenance cost require project history, defect data, CI results, or other measurements. We do not have those. The test suite's assertion gaps can be discussed as risks, but they do not prove that a production defect escaped or that a test was lost.

## Questions to practice aloud

What does dependency injection mean here? The service receives its collaborators in the constructor, so construction is explicit and tests can provide controlled objects.

What does the adapter do? It implements the application's motor interface and translates the call to the legacy motor API.

Why is the sensor test weak? It accepts zero even though the setup initializes the sensor to one.

Why is the CSV test weak? The fixture chooses the same floor the elevator starts on, so it does not show a changed destination.

Is a smell a bug? No. It is a sign that understanding, maintaining, or testing the code may be harder. A defect requires evidence that behavior violates a requirement.

Why is the sensor mismatch not a confirmed bug? The intended contract is unclear; a real sensor might update independently, while the in-memory one does not.

Does complete coverage prove the program is correct? No. Coverage records execution, not the correctness of the oracle or the completeness of requirements.

What should a strong finding include? The file and method, the exact evidence, the course concept, why it matters, and a proportionate improvement direction.

## Closing mental model

Remember the project in layers. The model stores elevator state. The service coordinates requests. Hardware interfaces provide motor, door, and sensor behavior. The factory wires those pieces from configuration. The adapter makes the legacy motor fit the expected interface. The event publisher notifies listeners. The tests exercise selected workflows and expose examples of weak or redundant test design.

Our strongest evidence is the sensor test that accepts two outcomes, the broad movement-and-door test, the fixture scenario that starts and ends on floor one, and the near-duplicate recording-motor tests. Our most important caution is to distinguish evidence from inference. Explain what the code shows, what the course calls it, what risk it creates, and what requirement would resolve any uncertainty. That is the core of a precise CA3 review.
