# Implementation Tasks: Move Running API

**Feature**: Move Running API  
**Branch**: `001-move-running-api`  
**Date**: January 19, 2026  
**Status**: Ready for Implementation

---

## Overview

This document breaks down the implementation of the Move Running API into actionable, dependency-ordered tasks organized by user story. The API enables developers to execute Move implementations on planning solutions in both permanent and temporary modes for testing purposes.

**Total Tasks**: 18  
**Estimated Effort**: 2-3 days  
**MVP Scope**: User Story 1 only (7 tasks)

---

## Implementation Strategy

### MVP First (User Story 1)
Focus on permanent move execution with basic infrastructure. This provides immediate value and validates the core architecture before adding temporary execution complexity.

### Incremental Delivery
Each user story phase represents a complete, independently testable increment:
- **User Story 1** (P1): Permanent execution → Tests with actual moves
- **User Story 2** (P2): Temporary execution with undo → Full feature set

### Parallel Opportunities
Tasks marked with `[P]` can be executed in parallel with previous tasks when they operate on different files and have no dependencies on incomplete work.

---

## Phase 1: Setup & Infrastructure

**Goal**: Initialize project structure and add required InnerScoreDirector method

### Tasks

- [ ] T001 Add new method to InnerScoreDirector interface in core/src/main/java/ai/timefold/solver/core/impl/score/director/InnerScoreDirector.java
  - Add: `<Result_> Result_ executeTemporaryMove(Move<Solution_> move, Function<Solution_, Result_> callback);`
  - Add Javadoc explaining the method executes move, invokes callback, undoes changes, returns result

- [ ] T002 Implement executeTemporaryMove in AbstractScoreDirector in core/src/main/java/ai/timefold/solver/core/impl/score/director/AbstractScoreDirector.java
  - Delegate to `moveDirector.executeTemporary(move, (score, undoMove) -> callback.apply(workingSolution))`
  - Undo handled automatically by moveDirector infrastructure

- [ ] T003 Create package-info.java for Preview API in core/src/main/java/ai/timefold/solver/core/preview/api/move/package-info.java
  - Mark package as @NullMarked (JSpecify)
  - Document that this is Preview API (subject to evolution with migration support)
  - Include warning about backward compatibility not guaranteed

---

## Phase 2: User Story 1 - Execute Move on Solution (P1)

**Goal**: Enable developers to execute a Move permanently on a solution and verify changes with their own assertions.

**Independent Test**: Execute a simple swap move, verify entities exchanged values using standard assertions.

**Success Criteria**:
- Developers can execute custom moves via `MoveRunner.using(solution).execute(move)`
- Move's execute() method is invoked on the solution
- Solution state is modified as expected
- Changes persist after execution
- Resources are properly cleaned up

### Tasks

- [ ] T004 [US1] Create MoveRunner class in core/src/main/java/ai/timefold/solver/core/preview/api/move/MoveRunner.java
  - Add @NullMarked annotation
  - Add private field: `InnerScoreDirector<Solution_, ?> scoreDirector`
  - Add private field: `boolean closed = false`
  - Add private constructor taking scoreDirector

- [ ] T005 [US1] Implement static factory method using() in MoveRunner.java
  - Signature: `public static <Solution_> MoveRunner<Solution_> using(Solution_ solution)`
  - Use Objects.requireNonNull(solution, "solution")
  - Get SolutionDescriptor via `SolutionDescriptor.buildSolutionDescriptor(solution.getClass())`
  - Create ConstraintStreamScoreDirectorFactory with dummy constraint (a minimal valid constraint using the first entity class from the solution descriptor; the constraint content doesn't affect MoveRunner functionality, e.g., `constraintFactory.forEach(FirstEntity.class).penalize(ONE)`)
  - Build InnerScoreDirector via scoreDirectorFactory.buildScoreDirector(false, false)
  - Call setWorkingSolution(solution) to initialize shadow variables
  - Return new MoveRunner instance

- [ ] T006 [P] [US1] Implement execute(move) method in MoveRunner.java
  - Signature: `public void execute(Move<Solution_> move)`
  - Validate not closed (throw IllegalStateException if closed)
  - Use Objects.requireNonNull(move, "move")
  - Delegate to scoreDirector.executeMove(move)
  - Add complete Javadoc with @param and examples

- [ ] T007 [P] [US1] Implement execute(move, handler) method in MoveRunner.java
  - Signature: `public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler)`
  - Validate not closed
  - Use Objects.requireNonNull for both parameters
  - Wrap scoreDirector.executeMove(move) in try-catch
  - Invoke exceptionHandler.accept(e) on exception (suppress propagation)
  - Add complete Javadoc

- [ ] T008 [P] [US1] Implement close() method in MoveRunner.java
  - Signature: `public void close() throws Exception` (AutoCloseable)
  - Check if already closed, set closed = true
  - Delegate to scoreDirector.close()
  - Add Javadoc explaining resource cleanup requirement

- [ ] T009 [US1] Create unit test MoveRunnerTest.java in core/src/test/java/ai/timefold/solver/core/preview/api/move/MoveRunnerTest.java
  - Test using() factory method creates runner successfully
  - Test execute() invokes move and modifies solution
  - Test execute() with null move throws NullPointerException
  - Test execute() after close throws IllegalStateException
  - Test execute(move, handler) suppresses exceptions when handler provided
  - Test close() can be called multiple times safely
  - Use AssertJ for all assertions (mandatory)
  - Use try-with-resources pattern in all tests

- [ ] T010 [US1] Create integration test MoveRunnerIT.java in core/src/test/java/ai/timefold/solver/core/preview/api/move/MoveRunnerIT.java
  - Test with real Move implementation (e.g., ListChangeMove or similar from solver core)
  - Test with complete solution including shadow variables
  - Verify shadow variables are initialized at construction
  - Verify shadow variables update after move execution
  - Use AssertJ for all assertions

---

## Phase 3: User Story 2 - Temporary Move Execution with Undo (P2)

**Goal**: Enable developers to test moves temporarily with automatic state restoration.

**Independent Test**: Execute move temporarily, verify changes during scope, confirm restoration after.

**Success Criteria**:
- Developers can execute moves temporarily via `executeTemporarily(move, assertions)`
- User assertions run during temporary scope
- Solution fully restored to original state after normal execution
- Shadow variables reverted
- Return value variant works for extracting computed values

### Tasks

- [ ] T011 [US2] Implement executeTemporarily(move, assertions) in MoveRunner.java
  - Signature: `public void executeTemporarily(Move<Solution_> move, Consumer<Solution_> assertions)`
  - Validate not closed
  - Use Objects.requireNonNull for both parameters
  - Call scoreDirector.executeTemporaryMove(move, solution -> { assertions.accept(solution); return null; })
  - Add complete Javadoc with examples showing assertions

- [ ] T012 [P] [US2] Implement executeTemporarily(move, function) in MoveRunner.java
  - Signature: `public <Result_> Result_ executeTemporarily(Move<Solution_> move, Function<Solution_, Result_> function)`
  - Validate not closed
  - Use Objects.requireNonNull for both parameters
  - Return scoreDirector.executeTemporaryMove(move, function)
  - Add Javadoc with example showing score extraction: `modifiedSolution.getScore()`

- [ ] T013 [US2] Add temporary execution tests to MoveRunnerTest.java
  - Test executeTemporarily() with void Consumer runs assertions
  - Test executeTemporarily() with Function returns computed value
  - Test solution is restored after temporary execution completes
  - Test with null move throws NullPointerException
  - Test with null callback throws NullPointerException
  - Test after close throws IllegalStateException
  - Test exception during temporary execution leaves solution in undefined state (FR-007a)
  - Verify solution state before, during, and after temporary execution

- [ ] T014 [US2] Add temporary execution integration test to MoveRunnerIT.java
  - Test with complex move affecting multiple entities
  - Test shadow variables are reverted after temporary execution
  - Test cascade effects are fully undone
  - Test extracting score via executeTemporarily(move, sol -> sol.getScore())
  - Verify no side effects on original solution

---

## Phase 4: Documentation & Polish

**Goal**: Complete public API documentation and ensure code quality.

### Tasks

- [ ] T015 Add comprehensive Javadoc to MoveRunner.java
  - Class-level Javadoc explaining purpose, Preview API status, try-with-resources requirement
  - Include usage examples for all three execution patterns (permanent, temporary void, temporary return)
  - Document thread safety (not thread-safe)
  - Document exception behavior for each method
  - Add @implNote in executeTemporarily methods clarifying that nesting is not supported and no runtime check is performed (developer responsibility)
  - Add @see references to Move interface and related concepts

- [ ] T016 Update package-info.java with complete API overview
  - Document MoveRunner as primary entry point
  - Explain Preview API evolution policy (subject to evolution with migration support)
  - Include complete example showing typical usage
  - Document limitations (single-threaded, no nested temporary execution)
  - Note that OpenRewrite migration recipes will be provided when the API evolves in future versions

- [ ] T017 Run code quality checks
  - Maven auto-format: `mvn spotless:apply`
  - Compile: `mvn clean compile -DskipTests`
  - Run tests: `mvn test -Dtest=MoveRunner*`
  - Verify SonarCloud gates would pass (local analysis if available)

- [ ] T018 Dogfooding validation (Success Criterion SC-007)
  - Use MoveRunner to test at least 2 different solver move types from core
  - Suggested: ListChangeMove and one other move type
  - Create example tests showing real-world usage
  - Verify MoveRunner works with actual solver moves

---

## Dependencies & Execution Order

### Story Completion Order

```
Phase 1 (Setup)
    ↓
Phase 2 (User Story 1) ← MVP Checkpoint
    ↓
Phase 3 (User Story 2) ← Complete Feature
    ↓
Phase 4 (Documentation & Polish)
```

**MVP Delivery**: Complete through Phase 2 for minimum viable product (permanent execution only).

**Full Feature**: Complete through Phase 3 for complete user story implementation.

### Task Dependencies

**Sequential (must complete in order)**:
- T001 → T002 (InnerScoreDirector method must exist before implementation)
- T003 → T004 (package-info before MoveRunner)
- T004 → T005 → T009 (class, factory, tests)
- T011/T012 → T013 (implementation before tests)

**Parallel Opportunities**:
- T006, T007, T008 can run in parallel after T005 (different methods)
- T009 and T010 can run in parallel after T008 (different test files)
- T011 and T012 can run in parallel after T010 (different methods)
- T013 and T014 can run in parallel after T012 (different test files)
- T015, T016, T017 can run in parallel after T014

### Parallel Execution Example (Per Story)

**User Story 1 Parallelization**:
```
T004 → T005 → {T006, T007, T008} → {T009, T010}
         └────────┬────────┘        └────┬────┘
           3 parallel tasks       2 parallel tasks
```

**User Story 2 Parallelization**:
```
{T011, T012} → {T013, T014}
└─────┬─────┘   └─────┬─────┘
  2 parallel      2 parallel
```

---

## Testing Strategy

### Unit Tests (MoveRunnerTest.java)
- **Scope**: Test MoveRunner behavior in isolation
- **Framework**: JUnit 5 + AssertJ (mandatory)
- **Coverage**: All public methods, exception paths, state transitions
- **Mocking**: Use Mockito for Move and ScoreDirector if needed

### Integration Tests (MoveRunnerIT.java)
- **Scope**: Test with real solver Move implementations
- **Framework**: JUnit 5 + AssertJ
- **Coverage**: Real moves, complex solutions, shadow variables
- **No Mocking**: Use actual solver infrastructure

### Dogfooding Tests
- **Scope**: Real-world usage with 2+ move types from solver core
- **Purpose**: Validate API usefulness (SC-007)
- **Location**: Separate test class or within existing move tests

---

## Success Validation

After completing all tasks, verify:

- ✅ **SC-001**: Developers can execute custom moves and verify changes with assertions
- ✅ **SC-002**: Temporary execution restores solution in 100% of normal flow test cases
- ✅ **SC-003**: Complete validation workflows possible without manual state management
- ✅ **SC-004**: Move execution workflow doesn't require understanding undo mechanisms
- ✅ **SC-005**: API supports all standard Move types
- ✅ **SC-006**: User assertions executable during temporary scope
- ✅ **SC-007**: API used to test at least 2 different solver move types (dogfooding)

### Acceptance Criteria

**User Story 1**:
- Move's execute() method invoked on solution ✅
- Solution's planning variables modified ✅
- Changes persist after execution ✅
- All changes visible for user assertions ✅

**User Story 2**:
- Solution modified during temporary scope ✅
- User assertions can verify changes ✅
- Solution state matches pre-execution state after completion ✅
- All changes reverted including cascade effects ✅

---

## Implementation Notes

### Critical Design Decisions

1. **InnerScoreDirector Extension**: New method `executeTemporaryMove(move, callback)` required
2. **Bootstrap Pattern**: Use ConstraintStreamScoreDirectorFactory with dummy constraint (single constraint using first entity from solution)
3. **No Score Calculation**: Users read scores via `solution.getScore()`, never calculate directly
4. **Resource Management**: AutoCloseable mandatory, try-with-resources enforced via documentation
5. **Preview API**: Package clearly marked, OpenRewrite migrations planned for evolution

### Common Pitfalls

- ❌ Don't calculate scores manually - always read from solution
- ❌ Don't forget try-with-resources - resources will leak
- ❌ Don't nest executeTemporarily calls - undefined behavior
- ❌ Don't share MoveRunner across threads - not thread-safe

### Code Quality Requirements

- **Nullability**: @NullMarked on classes, Objects.requireNonNull for parameters
- **Testing**: AssertJ for ALL assertions (no JUnit assertions)
- **Formatting**: Maven spotless must pass
- **Documentation**: Complete Javadoc on all public methods
- **Constitution**: All 10 principles must pass (see plan.md)

---

## Timeline Estimate

**Phase 1 (Setup)**: 0.5 days  
**Phase 2 (User Story 1)**: 1 day  
**Phase 3 (User Story 2)**: 0.5 days  
**Phase 4 (Polish)**: 0.5 days  

**Total**: 2.5 days

**MVP (Phase 1-2)**: 1.5 days

---

## Next Steps

1. Create feature branch: `git checkout -b 001-move-running-api`
2. Start with Phase 1 (Setup) - complete all T001-T003
3. Implement MVP (Phase 2) - complete T004-T010
4. Test MVP with real moves before proceeding
5. Complete full feature (Phase 3) - complete T011-T014
6. Polish and validate (Phase 4) - complete T015-T018

---

**Generated**: January 19, 2026  
**Last Updated**: January 19, 2026  
**Status**: Ready for Implementation ✅
