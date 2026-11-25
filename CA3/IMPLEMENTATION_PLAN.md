# CA3 Agent Implementation Plan

## Objective and scope

Review the supplied elevator simulator and its pre-written tests as a software-testing reviewer. Identify and explain a defensible selection of course-taught code smells, behavioral smells, testability concerns, and design patterns. Deliver a clear written report and CA2-style project documentation adapted to this review assignment.

**This is a review task. Do not refactor or fix production code or rewrite the existing tests.** The CA3 README explicitly says no fixes are expected. Preserve the supplied behavior and test suite; report observations, their evidence, likely impact, and possible improvement directions without implementing them.

The assignment says the student should learn and explain the review, so the report must be understandable to the team and support an oral question-and-answer session. AI assistance is permitted by the handout, but the students remain responsible for explaining findings.

## Repository facts established during inspection

- Java 17 Maven project using JUnit Jupiter 5.10.2 and Surefire 3.2.5.
- Production packages: `config`, `events`, `hardware`, `model`, and `service`.
- Existing test classes: `ElevatorServiceTest`, `FactoryTest`, and `LegacyPanelTest`.
- Existing test fixture: `src/test/resources/floors.csv`.
- Runtime configuration: `src/main/resources/elevator.properties`, currently selecting initial floor 1 and the legacy motor adapter.
- The README calls this a review target and explicitly says fixes are not expected.
- The assignment PDF asks for a typed PDF report named `CA3_Sid1_Sid2.pdf`, completed in a team of two. The report must identify code smells and test-design patterns from the taught material. It says the code contains examples of the items from the course slides and does not introduce new categories.

## Course material to use

Use the supplied course slides as the vocabulary and classification boundary:

- **12 Design for Testability:** observability, controllability, decomposability, simplicity, stability; dependency injection, inversion of control, focused interfaces, adapters, configuration, and observer/event patterns; hidden dependencies, god objects, tight coupling, global state, nondeterminism, and excessive private-helper structure.
- **13 Code Smells:** obscure tests, eager tests, mystery guests, general fixtures, irrelevant or hard-coded data, indirect testing, conditional test logic, production logic in tests, multiple test conditions, hard-to-test code, duplicated test code, and test logic in production.
- **14 Behavioral Smells:** assertion roulette, erratic/interacting/unrepeatable tests, fragile and overspecified tests, frequent debugging, manual intervention, and slow tests.
- **15 Project Smells:** buggy or missing tests, maintenance cost, escaped production defects, lost tests, and project-health metrics/risk categories.
- **16 Test Coverage Criteria:** statement, branch, path, condition and MC/DC criteria; coverage requirements; infeasible requirements; subsumption; and the limitation that coverage percentage is not correctness or confidence.

Do not label a code detail with a smell just because it looks unusual. Tie each classification to the slide definition and cite direct evidence. Distinguish a smell (a warning sign) from a confirmed functional defect, a design pattern (an intentional structure), and a possible improvement. A smell is not automatically a bug.

## Phase-by-phase execution

### Phase 1: Freeze the review boundary and establish the inventory

1. Re-read the assignment PDF and README before analysis.
2. Record all source, test, configuration, resource, and project files. Confirm the three test classes and the two runtime/test resource files listed above.
3. Do not edit Java production or test files. Do not add new tests or alter expected test results.
4. Treat a Maven test run as optional evidence only. Do not claim a test pass or failure without execution. If execution is authorized later, record the exact command and result separately from static review findings.

**Phase output:** a short scope note in the report stating that this is a static review of supplied code and pre-written tests, and that no code changes were made.

### Phase 2: Build a compact system and dependency map

Trace the actual assembly and runtime path:

1. `ElevatorConfiguration` loads `elevator.properties` from the classpath.
2. `ElevatorFactory` reads the initial floor and motor type, builds the elevator, selects a motor implementation, creates door and sensor components, wires an event publisher, and returns `ElevatorService`.
3. `ElevatorService` coordinates the elevator model, motor, door, floor sensor, and event publisher.
4. `LegacyMotorAdapter` adapts `LegacyLiftMotor` to the `Motor` interface; `RecordingMotor` is another `Motor` implementation.
5. `ElevatorEventPublisher` notifies `ElevatorEventListener` subscribers. `SimpleDoor` implements both `DoorOpener` and `DoorStatusReader`.

Represent this in `ARCH.md` using a small Mermaid diagram and concise responsibility notes. Mark the system under review and the test boundary accurately. Do not imply that an external physical elevator or integration environment exists.

**Phase output:** an architecture map that makes dependencies, configuration, event flow, and adapter selection understandable before smell findings are discussed.

### Phase 3: Review production code for testability and taught patterns

Inspect each production class and record evidence with file and line locations. Consider these as investigation prompts, not pre-decided findings:

- **Dependency injection:** `ElevatorService` receives its collaborators in its constructor. Describe how this improves controllability and decomposability for tests.
- **Factory / configuration:** `ElevatorFactory` centralizes object construction and selects a motor from properties. Discuss its wiring role, what is configurable, and any coupling or coverage consequence without treating the pattern itself as a smell.
- **Adapter:** `LegacyMotorAdapter` maps `Motor.moveTo` to the legacy `selectDestinationAndStart` API. Explain the incompatible interfaces it bridges.
- **Observer / event notification:** `ElevatorEventPublisher` and `ElevatorEventListener` separate event production from subscribers. Explain the indirection and its test implications.
- **Focused interfaces:** `Motor`, `FloorSensor`, `DoorOpener`, and `DoorStatusReader` expose small capabilities. Inspect how these aid substitution, and separately note that `ElevatorService` uses an `instanceof DoorStatusReader` check to discover an optional door capability. Classify this only if the course definitions support the claim; otherwise report it as a design trade-off.
- **Potential testability or behavior concerns:** inspect factory hard-coding, mutable state access through `getElevator`, public `setState`, unchecked constructor collaborators, the door check, movement ordering, exception paths, sensor updates, and event listener failures. Separate confirmed source behavior from assumptions about intended requirements. In particular, verify whether the factory's `InMemoryFloorSensor` is updated after movement before claiming that `getCurrentFloor()` becomes stale; state the observed behavior and its conditions precisely.

For each selected production finding, capture: location, observed code, applicable course concept, why it fits, impact/risk, and a non-implemented improvement direction. Do not describe a design pattern as a smell merely because it adds indirection.

### Phase 4: Review the existing tests against test-smell material

Read all three test classes method by method. Make a test matrix with columns for test name, scenario, setup/data source, action, expected result, smell or good practice under review, and evidence.

Investigate candidate findings from the source, while confirming each against the slides:

- `ElevatorServiceTest.testMoveOpenCloseAndNotifyEverything` combines movement, door operations, states, and events in one method. Consider the course's eager-test / single-condition guidance and whether failure localization is difficult.
- `testMoveToFloorFromFixtureFile` reads external fixture data and uses the first CSV row, which is floor 1 while the service fixture also begins at floor 1. Examine whether this is a Mystery Guest, hard-coded/external fixture dependency, and weak behavioral check. Explain exactly what behavior it does and does not establish.
- `testSensorReadingWithConditionalVerification` branches on the observed value and accepts different expected values. Assess it as conditional verification logic and explain how the current fixture makes the branch behave.
- `testSeveralDestinationsInOneMethod` repeats one operation/assertion for three values. Assess the multiple-test-conditions pattern and whether parameterized tests would make cases clearer; do not call repetition a defect by itself.
- `testMoveToHardCodedFloor`, CSV-backed movement, and `testAnotherRecordingMotorScenario` use fixed data or repeat nearby scenarios. Identify only the relevant hard-coded-data or duplication concerns, with evidence and measured impact.
- Check whether broad fixtures, magic values, vague test names, weak assertions, uncovered branches, or implementation-coupled assertions apply. Do not invent a smell where the example does not fit.
- Review `FactoryTest` and `LegacyPanelTest` separately. Distinguish component behavior checks from factory wiring checks. Note whether both motor-selection branches, exception paths, boundaries, and event behavior are exercised, but distinguish missing coverage from a smell.

Review test quality with the behavioral-smell slides too: ask whether a test is deterministic, isolated, self-checking, free of manual steps, and fast. Since the supplied tests use fresh local objects and no delays, do not claim erratic, manual-intervention, or slow-test smells without evidence.

### Phase 5: Map tests to behavior and coverage requirements

Create a concise trace from important behaviors/branches to existing tests. At minimum inspect:

- factory motor selection for `legacy` versus non-legacy configuration;
- movement success, invalid floor, and open-door rejection;
- state transitions and event publication for move/open/close;
- sensor reads and whether they reflect movement;
- adapter and recording motor behavior;
- event listener behavior, including no-listener and listener-failure cases.

Use the coverage slides to explain untested branches or behaviors. Do not claim a measured coverage percentage unless a coverage tool is actually run and its report is reviewed. Clarify that statement/branch/path/condition criteria answer different questions; path coverage can grow rapidly; infeasible requirements exist; and high coverage alone does not establish correct expected results.

### Phase 6: Validate and prioritize findings

For every proposed finding, ask:

1. Is there direct evidence in the current source or test?
2. Does the course slide define this smell, pattern, or criterion?
3. Is it a smell, a functional defect, a missing test requirement, or an optional design trade-off?
4. What maintenance, reliability, diagnosability, isolation, or testability risk does it create?
5. What is a proportionate recommendation that preserves the assignment boundary?

Prioritize findings by impact and confidence. Include meaningful good patterns as well as problems. Avoid a long unranked list and avoid claiming that every possible smell must be present; the handout explicitly allows teams to identify a selection.

### Phase 7: Produce the report and CA2-style deliverables

Create the following CA3-relevant files:

1. **`README.md`** — replace the placeholder overview with purpose, architecture summary, how to run the existing suite, review scope, and links to `ARCH.md` and reports. Preserve the key statement that the assignment is a review and no fixes are expected.
2. **`ARCH.md`** — system/dependency map, runtime flow, configuration and pattern roles, and review boundary. Keep smell findings in the report rather than making architecture notes an unstructured issue list.
3. **`CA3_Report.md`** — editable source for the typed review report. Organize it as: assignment scope and method; architecture; design patterns; prioritized production-code smells; test-code and behavioral smells; behavior-to-test/coverage map; recommendations; limitations and conclusion. Each finding should cite file/class/method and line numbers and give evidence, course definition, impact, and proposed direction. Clearly state that the review did not change code.
4. **`report.docx`** — optional convenient rendered copy of the report source, matching the CA2 documentation set if document generation is available.
5. **`CA3_Sid1_Sid2.pdf`** — required typed PDF per the assignment. Render/inspect every page for layout and ensure findings remain readable. Student IDs are not present in the repository; preserve a clearly marked naming placeholder in the plan/report until the team supplies them, then use the exact required filename.
6. **`report-audio.md`** — optional study guide modeled on CA2, with an audio-friendly explanation of the assignment, architecture, key smell/pattern findings, coverage distinctions, and oral-review questions. It supplements the required report; it does not replace the typed PDF.

Keep `pom.xml`, `.gitignore`, and `LICENSE` unless inspection reveals a specific CA3-relevant correction. Do not add a new test suite or change pre-written tests for this review assignment.

### Phase 8: Final review and handoff

- Re-check every report claim against the actual file and line.
- Confirm smell and pattern names match the supplied course material and recommendations remain suggestions, not implemented changes.
- Confirm all relevant existing tests are represented in the test matrix and important untested branches are stated as coverage gaps rather than automatically as defects.
- Ensure `README.md`, `ARCH.md`, report source, optional DOCX, required PDF, and optional audio guide agree on scope and facts.
- Leave the Java source and tests untouched. Report any execution result only if the agent actually ran the command and was authorized to do so.

## Acceptance criteria

- The assignment remains a review: no production or test code fixes are included.
- Findings have traceable code/test evidence and course-grounded terminology.
- Patterns are identified as intentional designs with their roles; smells and defects are distinguished.
- Test quality, architecture/testability, and coverage gaps are all considered.
- The required typed PDF exists with the correct team-specific filename, and CA2-style project documentation is adapted to CA3 rather than copied unchanged.
- The report is usable for the team's oral explanation of why each finding is valid and what its consequences are.
