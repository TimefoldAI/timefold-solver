# Implementation Plan: Move Testing API

**Branch**: `001-move-testing-api` | **Date**: January 19, 2026 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-move-testing-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Provide a testing API (`MoveRunner`) that allows developers to execute Move implementations on planning solutions for validation purposes. The API supports:
- **Permanent execution**: Changes persist after execution - `MoveRunner.on(solution).execute(move)` or with optional exception handler `execute(move, exceptionHandler)`
- **Temporary execution**: Automatic undo after user assertions callback - `executeTemporarily(move, assertions -> {...})`
- **Simple API**: Static factory method returns runner instance, no separate builder class
- **Optional exception handling**: Via optional parameter on `execute(move, Consumer<Exception>)` method
- **Full solution requirement**: A complete planning solution instance must always be provided
- **Undo mechanism**: Leverages solver's existing undo mechanisms for state restoration

## Technical Context

**Language/Version**: Java 17 (compile-time compatibility), supports latest JDK runtime  
**Primary Dependencies**: None (production code), JUnit + AssertJ + Mockito (test infrastructure)  
**Storage**: N/A (testing API - in-memory only)  
**Testing**: JUnit 5 with AssertJ assertions  
**Target Platform**: JVM (JDK 17+)  
**Project Type**: Single project (constraint solver library)  
**Performance Goals**: Minimal overhead for move execution; undo operations complete when they complete (no specific constraint)  
**Constraints**: Zero production dependencies policy; API designed for testing only (single-threaded, synchronous)  
**Scale/Scope**: Core module API addition for testing solver's own moves (dogfooding); single class API with 2-3 public methods

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Real World Usefulness (MUST)
- [ ] Used in at least one quickstart → **PLAN**: Add MoveRunner usage example to test documentation/quickstart
- [ ] Used internally to test solver's own moves → **PLAN**: Dogfooding - use API in core module tests
- [ ] Migrate existing move tests to use MoveRunner → **PLAN**: Replace existing test patterns with MoveRunner API
- [ ] Fully tested (unit, integration) → **PLAN**: Comprehensive test suite in core module
- [ ] Fully documented (Javadoc, user guide) → **PLAN**: Complete Javadoc on public API classes/methods
- [ ] Note: `@since` tags NOT required per constitution

### ✅ Consistent Terminology (MUST)
- [ ] Variable naming includes collection type (e.g., `moveList` not `moves`) → **VERIFY** in code review
- [ ] Feature name "Move Testing API" / "MoveRunner" used consistently → **OK**

### ✅ Fail Fast (MUST)
- [ ] Null checks at API entry points → **PLAN**: Validate null Move/Solution in runner
- [ ] Invalid state detection (nested temporary execution) → **PLAN**: Track execution state, throw on nesting

### ✅ Understandable Error Messages (MUST)
- [ ] Include variable names and states in exceptions → **PLAN**: Follow constitution format
- [ ] Provide actionable advice with "maybe" suggestions → **PLAN**: Where applicable

### ✅ Automated Testing (MUST)
- [ ] Unit tests for all public methods → **PLAN**: Test suite in test module
- [ ] Integration tests for move execution scenarios → **PLAN**: Tests with actual solver moves
- [ ] Use JUnit + AssertJ (NO JUnit assertions) → **ENFORCE** in code review

### ✅ Code Hygiene (MUST)
- [ ] Automatic formatting via Maven build → **OK** (existing infrastructure)
- [ ] SonarCloud quality gates (B or better) → **VERIFY** in CI
- [ ] Code coverage maintained → **VERIFY** in CI
- [ ] Sparse newline usage → **ENFORCE** in code review
- [ ] Field ordering consistency → **ENFORCE** in code review

### ✅ Java Language Version (MUST)
- [ ] JDK 17 compile-time compatibility → **OK** (project standard)
- [ ] Use modern Java features (var, records, text blocks, pattern matching) → **PLAN** where appropriate
- [ ] NO Optional usage → **ENFORCE** in code review
- [ ] Use @NullMarked with explicit @Nullable → **PLAN**: Apply to public API classes

### ✅ Production Dependencies (MUST)
- [ ] ZERO external dependencies in production code → **OK** (testing API uses only JDK + solver internals)
- [ ] JSpecify for nullability annotations → **OK** (compile-time only)

### ✅ Test Infrastructure (MUST)
- [ ] JUnit for test execution → **OK**
- [ ] AssertJ for ALL assertions (NO JUnit assertions) → **ENFORCE** in code review
- [ ] Mockito allowed for mocking → **OK** if needed

### ✅ Security (MUST)
- [ ] No secrets in code → **N/A** (testing API)
- [ ] Input validation (null checks) → **PLAN**: Validate at runner entry points
- [ ] No sensitive data in logs → **N/A** (testing API)
- [ ] Use SLF4J for logging → **PLAN**: Minimal logging (solver provides existing mechanisms)

### ✅ Package Structure (MUST)
- [ ] API in package with "api" in name → **PLAN**: Place in core.preview.api.move package
- [ ] Preview API status → **PLAN**: Package includes "preview" to indicate evolving API with migration support
- [ ] 100% backwards compatibility for API → **COMMIT**: Semantic versioning, no breaking changes in minor versions
- [ ] Core module placement for dogfooding → **PLAN**: API lives in core to test solver's own moves

### ✅ Deprecation Policy (SHOULD)
- [ ] Not applicable (new feature) → **OK**

### ⚠️ Constitutional Deviations

**NONE** - All requirements can be met within constitutional constraints.

## Project Structure

### Documentation (this feature)

```text
specs/001-move-testing-api/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
core/
└── src/
    └── main/
        └── java/
            └── ai/
                └── timefold/
                    └── solver/
                        └── core/
                            └── preview/
                                └── api/
                                    └── move/
                                        ├── MoveRunner.java         # Main API class (no separate builder)
                                        └── package-info.java       # Package documentation
    └── test/
        └── java/
            └── ai/
                └── timefold/
                    └── solver/
                        └── core/
                            └── preview/
                                └── api/
                                    └── move/
                                        ├── MoveRunnerTest.java              # Unit tests
                                        ├── MoveRunnerIntegrationTest.java   # Integration tests
                                        └── MoveRunnerEdgeCasesTest.java     # Edge case tests
```

**Structure Decision**: This is a testing API addition to the **core** module in Timefold Solver. The implementation goes in `core/src/main/java` (testing utilities available to all users and for dogfooding solver's own moves) and tests go in `core/src/test/java`. The API package path `ai.timefold.solver.core.preview.api.move` includes "preview" to indicate this is a preview API (subject to change but with migration support) and "api" for public API status. The "move" subpackage clearly scopes this to move-related testing utilities. Single class design (MoveRunner) with no separate builder class. **Core module placement enables dogfooding** - the solver can use this API to test its own move implementations.

## Complexity Tracking

**No constitutional violations** - This feature complies with all MUST and SHOULD requirements.

## Phase 2: Implementation Details

### Key Design Decisions (Updated from Spec Clarifications)

#### 1. API Pattern: Simple Static Factory with Optional Parameters
**Decision**: Use static factory method returning runner instance; no separate builder class

**API Surface**:
```java
// Permanent execution without exception handler
MoveRunner.on(solution).execute(move);

// Permanent execution with exception handler (optional parameter)
MoveRunner.on(solution).execute(move, exceptionHandler);

// Temporary execution with assertions
MoveRunner.on(solution).executeTemporarily(move, assertions -> {
    // User assertions here - exceptions handled within callback
    assertThat(solution.getEntity(0).getValue()).isEqualTo(expected);
});
```

**Rationale** (from spec Session 2026-01-19 + user feedback):
- Clear, simple API without builder complexity
- Static factory method `MoveRunner.on(solution)` creates runner instance
- Exception handler is optional parameter on `execute()` method (not separate builder call)
- Temporary execution doesn't need exception handler parameter (handle within assertions callback)
- Single class design - no separate builder class needed
- Allows future extensions through method overloads

#### 2. Full Solution Requirement
**Decision**: API MUST always receive a complete planning solution instance

**Rationale** (from spec Session 2026-01-19):
- Entity value ranges and other constraints prevent creation of completely dummy solution
- Attempting to support entity-only testing adds significant complexity
- Users must provide actual solution instances with proper configuration

**Implementation**:
- Remove `of(entities, facts, valueRange)` overloads from original research
- Single entry point: `MoveRunner.on(Solution solution)`
- Validate solution is non-null at entry point

#### 3. Exception Handling
**Decision**: Optional Consumer parameter on execute() method for permanent execution; callback-based for temporary

**Specification**:
- **FR-001a**: Optional `Consumer<Exception>` as parameter to `execute(move, handler)` method
- **FR-013**: Propagate exceptions to handler (if provided) or caller; no automatic rollback
- Temporary execution: exceptions handled within assertions callback (Consumer parameter)
- **Exception handler parameter MUST NOT be nullable** - if the overload with handler is used, a real value must be provided

**Implementation**:
```java
public final class MoveRunner<Solution_> {
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    
    // Method overload without exception handler
    public void execute(Move<Solution_> move) {
        // Validate move is non-null
        // Execute move, propagate exceptions to caller
    }
    
    // Method overload with exception handler
    public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler) {
        // Validate move and exceptionHandler are both non-null
        try {
            // Execute move
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }
}
```

#### 4. Nested Temporary Execution
**Decision**: Throw exception if temporary execution attempted while already in temporary scope

**Specification** (FR-005a):
- MUST throw exception on nested temporary execution
- Not supported due to complexity and unclear use case

**Implementation**:
- Track boolean flag `inTemporaryScope` in runner instance
- Check before entering `executeTemporarily()`
- Throw `IllegalStateException` with clear message:
  ```java
  throw new IllegalStateException(
      "Nested temporary execution is not supported. " +
      "A temporary execution is already in progress.");
  ```

#### 5. Thread Safety
**Decision**: NOT thread-safe; single-threaded use only

**Specification** (Assumptions section):
- MoveRunner API is not thread-safe
- Designed for single-threaded test scenarios
- Each thread should create its own instance for parallel tests

**Documentation**:
- Clearly document in class-level Javadoc
- No synchronization overhead in implementation

#### 6. Shadow Variable Handling
**Decision**: Score director handles shadow variables transparently; no API-specific logic needed

**Specification**:
- **FR-012**: Trigger shadow variable updates when moves modify source variables
- **FR-012a**: MUST NOT suppress or interfere with listeners, shadow updates, or subsystems
- **FR-012b**: Initialize uninitialized shadow variables before move execution

**Implementation**:
- Leverage `InnerScoreDirector` - it handles shadow variable updates automatically
- No custom shadow variable logic in API code
- Score director transparently manages initialization, updates, and reversion

#### 7. Logging Strategy
**Decision**: No API-specific logging; rely on solver's existing mechanisms

**Specification** (FR-015):
- MUST NOT add API-specific logging
- Debugging visibility through solver's existing logging mechanisms

**Implementation**:
- No SLF4J logger instances in MoveRunner classes
- Document that users can enable solver logging for debugging
- Keep API lightweight and focused

#### 8. Performance Constraints
**Decision**: No specific performance requirements for undo operations

**Specification** (Assumptions section):
- Undo operations have no specific performance constraints
- Restoration time depends on solution complexity and underlying solver mechanisms

**Implementation**:
- Accept solver's native undo performance characteristics
- No caching or optimization attempts
- Focus on correctness over performance

#### 9. Dogfooding: Migrate Existing Move Tests
**Decision**: Replace existing move test patterns with MoveRunner API

**Rationale**:
- Validates API usefulness through real-world usage on solver's own moves
- Ensures API is practical and complete for complex move testing scenarios
- Identifies missing features or usability issues early
- Demonstrates best practices for users
- Constitution requirement: features must be used in real scenarios

**Scope**:
- Identify all existing move tests in core module
- Replace manual move execution and state management with MoveRunner API
- Use `execute()` for permanent execution tests
- Use `executeTemporarily()` for tests that need state restoration
- Maintain existing test coverage and assertions
- Document migration patterns for user reference

**Implementation Approach**:
```java
// BEFORE: Manual move execution
var scoreDirector = buildScoreDirector(...);
var move = new SwapMove(...);
move.doMove(scoreDirector);
// manual assertions
scoreDirector.close();

// AFTER: Using MoveRunner API
MoveRunner.on(solution).execute(move);
// same assertions

// BEFORE: Manual temporary execution with undo
var scoreDirector = buildScoreDirector(...);
var move = new SwapMove(...);
var undoMove = move.doMove(scoreDirector);
// temporary assertions
undoMove.doMove(scoreDirector);
scoreDirector.close();

// AFTER: Using MoveRunner API
MoveRunner.on(solution).executeTemporarily(move, s -> {
    // temporary assertions
});
// automatic undo
```

**Benefits**:
- Simplifies test code (less boilerplate)
- Standardizes move testing patterns across codebase
- Validates API design against real-world complexity
- Provides migration examples for external users
- Improves test maintainability

### Updated Class Structure

Based on spec clarifications and user feedback, the class structure is:

1. **MoveRunner** - Single API class with static factory method and execution methods (no separate builder)
2. **Internal implementation** - Leverages solver's `InnerScoreDirector`, `MoveDirector`, etc.

See `data-model.md` for detailed class structure (NOTE: May need updating to reflect spec changes).

### Constitution Re-Check Post-Design

All constitutional requirements remain satisfied:
- ✅ Zero production dependencies (JDK + solver internals only)
- ✅ Simple API pattern is idiomatic Java (JDK 17 compatible)
- ✅ Consumer interface is standard JDK (no Optional usage)
- ✅ Clear fail-fast validation at API boundaries
- ✅ Package includes "api" for backwards compatibility guarantee
- ✅ @NullMarked with explicit @Nullable where needed
- ✅ Comprehensive testing planned (JUnit + AssertJ)

### Next Steps

1. **Review data-model.md**: Update to reflect spec changes (remove entity-only testing support, update API signatures to MoveRunner)
2. **Review contracts/**: Ensure API contracts match updated simple API pattern
3. **Review quickstart.md**: Verify examples use correct API (`MoveRunner.on(solution)`)
4. **Identify existing move tests**: Audit core module for move test patterns that should migrate to MoveRunner, create list for task breakdown
5. **Create tasks**: Run `/speckit.tasks` to break down implementation into actionable tasks (including migration tasks)

---

**Plan Status**: ✅ **Complete** - Ready for task breakdown

**Changes from Original Research**:
- Removed entity-only testing support (full solution always required)
- Updated exception handling to use Consumer pattern
- Clarified nested temporary execution prohibition
- Confirmed thread safety constraints
- Simplified API surface based on requirements
- **Moved from test module to core module for dogfooding** - enables testing solver's own moves

