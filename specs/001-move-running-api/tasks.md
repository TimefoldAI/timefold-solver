---
description: "Task list for Move Running API implementation"
---

# Tasks: Move Running API

**Input**: Design documents from `/specs/001-move-running-api/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included per constitution requirement for automated testing (all code must have automated tests).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- Production code: `core/src/main/java/ai/timefold/solver/core/preview/api/move/`
- Test code: `core/src/test/java/ai/timefold/solver/core/preview/api/move/`
- Implementation code: `core/src/main/java/ai/timefold/solver/core/impl/score/director/`
- Documentation: `docs/src/modules/ROOT/pages/optimization-algorithms/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for the new API

- [X] T001 Create package directory structure at `core/src/main/java/ai/timefold/solver/core/preview/api/move/`
- [X] T002 Create test package directory structure at `core/src/test/java/ai/timefold/solver/core/preview/api/move/`
- [X] T003 [P] Create builtin test subdirectory at `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Modify InnerScoreDirector to add executeTemporarily(move, consumer) variant in `core/src/main/java/ai/timefold/solver/core/impl/score/director/InnerScoreDirector.java`
- [X] T005 Update package-info.java with API documentation at `core/src/main/java/ai/timefold/solver/core/preview/api/move/package-info.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Execute Move on Solution (Priority: P1) 🎯 MVP

**Goal**: Enable developers to execute custom moves on planning solutions and verify behavior using their own test assertions. This is the core value proposition of the API.

**Independent Test**: Provide a simple swap move on a basic planning problem, execute it via the API, and use standard assertions to verify that two entities exchanged their planning variable values.

### Implementation for User Story 1

- [X] T006 [P] [US1] Create MoveRunner class with build() factory method, AutoCloseable implementation, and resource management in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunner.java`
- [X] T007 [P] [US1] Create MoveRunContext class with execute(move) method for permanent execution in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunContext.java`
- [X] T008 [US1] Implement MoveRunner.build() to construct SolutionDescriptor and ScoreDirectorFactory with Constraint Streams dummy constraint (depends on T006)
- [X] T009 [US1] Implement MoveRunner.using() to create MoveRunContext with fresh InnerScoreDirector from cached factory (depends on T006, T007, T008)
- [X] T010 [US1] Implement MoveRunContext.execute(move) for permanent move execution via InnerScoreDirector (depends on T007, T009)
- [X] T011 [US1] Implement MoveRunner.close() for resource cleanup and add closed state tracking (depends on T006)
- [X] T012 [US1] Add null validation for all public methods (solutionClass, entityClasses, solution, move) with appropriate exceptions (depends on T006, T007)
- [X] T013 [US1] Add IllegalStateException checks for closed MoveRunner usage in using() and MoveRunContext methods (depends on T006, T007, T011)

### Tests for User Story 1

- [X] T014 [P] [US1] Create MoveRunnerTest with basic unit tests for build() validation (null solutionClass, empty entityClasses) in `core/src/test/java/ai/timefold/solver/core/preview/api/move/MoveRunnerTest.java`
- [X] T015 [P] [US1] Add unit tests for using() validation (null solution) to MoveRunnerTest.java (depends on T014)
- [X] T016 [P] [US1] Add unit tests for execute(move) with simple custom move to verify move execution to MoveRunnerTest.java (depends on T014)
- [X] T017 [P] [US1] Add unit tests for closed MoveRunner IllegalStateException to MoveRunnerTest.java (depends on T014)
- [X] T018 [P] [US1] Add unit tests for resource cleanup verification to MoveRunnerTest.java (depends on T014)
- [ ] T019 [P] [US1] Create ChangeMoveTest for testing ChangeMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ChangeMoveTest.java`
- [ ] T020 [P] [US1] Create SwapMoveTest for testing SwapMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/SwapMoveTest.java`
- [ ] T021 [P] [US1] Create ListAssignMoveTest for testing ListAssignMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListAssignMoveTest.java`
- [ ] T022 [P] [US1] Create ListChangeMoveTest for testing ListChangeMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListChangeMoveTest.java`
- [ ] T023 [P] [US1] Create ListSwapMoveTest for testing ListSwapMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListSwapMoveTest.java`
- [ ] T024 [P] [US1] Create CompositeMoveTest for testing CompositeMove with MoveRunner API in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/CompositeMoveTest.java`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Developers can execute moves permanently and verify behavior using their own assertions.

---

## Phase 4: User Story 2 - Temporary Move Execution with Undo (Priority: P2)

**Goal**: Enable safe experimentation and validation without side effects - execute moves temporarily, validate intermediate state, and automatically restore the original solution state.

**Independent Test**: Execute a move temporarily, use assertions to verify changes are applied during the temporary scope, and confirm the solution is restored to its original state afterward.

### Implementation for User Story 2

- [X] T025 [US2] Implement MoveRunContext.executeTemporarily(move, assertions) using InnerScoreDirector.executeTemporarily() with consumer callback in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunContext.java`
- [X] T026 [US2] Implement MoveRunContext.execute(move, exceptionHandler) for permanent execution with exception handling (Consumer<Exception> signature, suppress Exception propagation, propagate Errors) in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunContext.java`
- [X] T027 [US2] Add fail-fast validation for null move and null callback parameters in executeTemporarily() before delegating to InnerScoreDirector (depends on T025)
- [X] T028 [US2] Add fail-fast validation for null exceptionHandler parameter in execute(move, exceptionHandler) before delegating to InnerScoreDirector (depends on T026)

### Tests for User Story 2

- [X] T029 [P] [US2] Add unit tests for executeTemporarily() with move execution and undo verification to MoveRunnerTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/MoveRunnerTest.java`
- [X] T030 [P] [US2] Add unit tests for executeTemporarily() with null move and null callback validation to MoveRunnerTest.java
- [X] T031 [P] [US2] Add unit tests for executeTemporarily() with complex move affecting multiple entities to verify complete state restoration to MoveRunnerTest.java
- [X] T032 [P] [US2] Add unit tests for execute(move, exceptionHandler) with Exception handling and suppression to MoveRunnerTest.java
- [X] T033 [P] [US2] Add unit tests for execute(move, exceptionHandler) with Error propagation (not suppressed) to MoveRunnerTest.java
- [X] T034 [P] [US2] Add temporary execution tests to ChangeMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ChangeMoveTest.java`
- [X] T035 [P] [US2] Add temporary execution tests to SwapMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/SwapMoveTest.java`
- [X] T036 [P] [US2] Add temporary execution tests to ListAssignMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListAssignMoveTest.java`
- [X] T037 [P] [US2] Add temporary execution tests to ListChangeMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListChangeMoveTest.java`
- [X] T038 [P] [US2] Add temporary execution tests to ListSwapMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/ListSwapMoveTest.java`
- [X] T039 [P] [US2] Add temporary execution tests to CompositeMoveTest.java in `core/src/test/java/ai/timefold/solver/core/preview/api/move/builtin/CompositeMoveTest.java`

**Checkpoint**: At this point, both User Stories 1 AND 2 should work independently. Developers can execute moves both permanently and temporarily with automatic undo.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, code quality improvements, and final validation

- [X] T040 [P] Add comprehensive Javadoc to MoveRunner class covering all public methods, @param, @return, @throws in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunner.java`
- [X] T041 [P] Add comprehensive Javadoc to MoveRunContext class covering all public methods, @param, @return, @throws in `core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunContext.java`
- [ ] T042 [P] Update user guide with MoveRunner API documentation and usage examples in `docs/src/modules/ROOT/pages/optimization-algorithms/neighborhoods.adoc`
- [X] T043 [P] Add @NullMarked annotations to all classes with explicit @Nullable where needed
- [X] T044 Code review for error message quality (include variable names and actionable advice)
- [X] T045 Verify constitution compliance (automated testing, fail-fast, understandable errors, code hygiene)
- [X] T046 Run Maven build to verify auto-formatting and compilation
- [ ] T047 Validate quickstart.md examples against actual implementation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion - Can start once Phase 2 is complete
- **User Story 2 (Phase 4)**: Depends on Foundational phase completion - Can start once Phase 2 is complete (may proceed in parallel with US1 if staffed)
- **Polish (Phase 5)**: Depends on User Stories 1 and 2 being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Extends US1 classes (MoveRunContext) but tests can run independently

### Within User Story 1

- T006, T007 can run in parallel (different classes)
- T008 depends on T006 (MoveRunner implementation)
- T009 depends on T006, T007, T008 (needs both classes and build() implementation)
- T010 depends on T007, T009 (needs MoveRunContext and score director setup)
- T011 depends on T006 (MoveRunner close() implementation)
- T012 depends on T006, T007 (validation for both classes)
- T013 depends on T006, T007, T011 (needs closed state tracking)
- All test tasks (T014-T024) can run in parallel after implementation is complete

### Within User Story 2

- T025, T026 can run in parallel (different methods in same class)
- T027 depends on T025 (validation for executeTemporarily)
- T028 depends on T026 (validation for execute with handler)
- All test tasks (T029-T039) can run in parallel after implementation is complete

### Negative Requirement Verification

Negative requirements (FR-014: no solution inspection, FR-015: no API logging) are verified by:
- **Code review** - Reviewers check for absence of prohibited functionality
- **Constitution compliance task** (T045) - Includes verification of "don't do" requirements
- No explicit tasks needed - violations would be caught in normal review process

### Parallel Opportunities

- Phase 1: All tasks (T001-T003) can run in parallel
- Phase 2: Tasks T004 and T005 can run in parallel
- User Story 1 Implementation: T006 and T007 can run in parallel
- User Story 1 Tests: T014-T024 can all run in parallel (11 parallel tests)
- User Story 2 Implementation: T025 and T026 can run in parallel
- User Story 2 Tests: T029-T039 can all run in parallel (11 parallel tests)
- Phase 5 Polish: T040, T041, T042, T043 can run in parallel

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all test creation tasks for User Story 1 together:
Task: "Create MoveRunnerTest with basic unit tests for build() validation"
Task: "Add unit tests for using() validation"
Task: "Add unit tests for execute(move) with simple custom move"
Task: "Add unit tests for closed MoveRunner IllegalStateException"
Task: "Add unit tests for resource cleanup verification"
Task: "Create ChangeMoveTest for testing ChangeMove with MoveRunner API"
Task: "Create SwapMoveTest for testing SwapMove with MoveRunner API"
Task: "Create ListAssignMoveTest for testing ListAssignMove with MoveRunner API"
Task: "Create ListChangeMoveTest for testing ListChangeMove with MoveRunner API"
Task: "Create ListSwapMoveTest for testing ListSwapMove with MoveRunner API"
Task: "Create CompositeMoveTest for testing CompositeMove with MoveRunner API"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently - developers can execute moves permanently
5. Ready for preview release with basic functionality

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Preview API ready for permanent execution (MVP!)
3. Add User Story 2 → Test independently → Complete API with temporary execution and exception handling
4. Polish → Production-ready API with full documentation
5. Each story adds value without breaking previous functionality

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (MoveRunner, MoveRunContext.execute())
   - Developer B: User Story 2 (MoveRunContext.executeTemporarily(), exception handling)
   - Developer C: Tests for both stories (can proceed in parallel once implementation is available)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Constitution requires automated testing for all code (tests are mandatory, not optional)
- Follow JUnit 5 + AssertJ convention (no JUnit assertions)
- Use @NullMarked on all classes, explicit @Nullable where needed
- Prefer `var` for local variables, diamond operator for generic types
- All public API requires comprehensive Javadoc
- Dogfooding requirement: Test at least 2 move types from builtin package (6 types included: ChangeMove, SwapMove, ListAssignMove, ListChangeMove, ListSwapMove, CompositeMove)
- Stop at any checkpoint to validate story independently
