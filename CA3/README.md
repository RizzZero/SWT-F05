# F05 CA3 Elevator Review

CA3 is a review exercise about code smells, test design, design patterns, and testability in a small Java elevator simulator. The implementation and JUnit tests are supplied as review material. The assignment expects a reasoned report and an oral explanation; it does not ask the team to repair or refactor the project.

## Start here

- [Implementation plan](IMPLEMENTATION_PLAN.md) describes the review phases and the required evidence.
- [Architecture and test notes](ARCH.md) explains how the simulator is wired and how the tests exercise it.
- [CA3 review report](CA3_Report.md) is the detailed editable report.
- [Audio study guide](report-audio.md) introduces the assignment and findings in a listenable order, then prepares the team for questions.
- `report.docx` and `CA3_Sid1_Sid2.pdf` are the formatted report deliverables. Replace `Sid1` and `Sid2` with the team's actual student identifiers before submission.

## Project map

- `src/main/java/edu/course/elevator/model`: elevator data and state.
- `src/main/java/edu/course/elevator/service`: movement, doors, and event coordination.
- `src/main/java/edu/course/elevator/hardware`: motor, sensor, and door interfaces and implementations, including a legacy motor adapter.
- `src/main/java/edu/course/elevator/events`: event listener and publisher.
- `src/main/java/edu/course/elevator/config`: properties loading and object construction.
- `src/test/java`: the pre-written JUnit 5 test suite under review.
- `src/main/resources/elevator.properties`: initial floor and motor selection.
- `src/test/resources/floors.csv`: input fixture used by one service test.

## Scope and method

The report uses the supplied assignment description and course slides as its classification vocabulary. It treats a smell as a warning sign rather than proof of a defect. It distinguishes test smells, production design tradeoffs, positive patterns, and hypotheses that need clarification. The source and test files remain unchanged because the assignment explicitly describes this as a review target.

The report does not claim that the tests pass: no test execution result is part of the static review. See [ARCH.md](ARCH.md) for the observable behavior and review limitations.

To run the existing suite from this directory when execution is desired, use `mvn test`. This documentation pass does not run it.
