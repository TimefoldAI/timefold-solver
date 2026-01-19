# Implementation Plan: Move Running API

**Branch**: `001-move-running-api` | **Date**: January 19, 2026 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-move-running-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement a testing utility API that enables developers to execute Move implementations on planning solutions in both permanent and temporary modes. The API provides a fluent builder pattern (`MoveRunner.build(solutionClass, entityClasses).using(solution).execute(move)`) for permanent execution and automatic undo support for temporary execution (`executeTemporarily(move, function)`). This is a **Preview API** designed for testing and development use cases, not production solving workflows.

**Key capabilities**:
- Build MoveRunner with solution class and entity classes (with validation)
- Execute moves permanently with optional exception handling
- Execute moves temporarily with automatic state restoration via solver's existing undo mechanisms
- Shadow variable initialization at MoveRunner construction time via solver architecture
- Resource management through AutoCloseable (mandatory try-with-resources)
- Single-threaded design for test scenarios

## Technical Context

**Language/Version**: Java (JDK 17 minimum, target JDK 17 for compilation, support latest JDK runtime)  
**Primary Dependencies**: 
- JSpecify (nullability annotations, compile-time only)
- Solver's existing core infrastructure (ScoreDirector, Move, MutableSolutionView)
**Storage**: N/A (in-memory testing API)  
**Testing**: JUnit 5 + AssertJ (mandatory per constitution) + Mockito for mocking  
**Target Platform**: JVM-based environments (JDK 17+)  
**Project Type**: Single project - Core solver module addition  
**Performance Goals**: O(changes) for execution and undo where changes = number of variable modifications; optimized for correctness in testing scenarios, not production performance  
**Constraints**: 
- Must use try-with-resources pattern (resources leak if not closed)
- Single-threaded design (not thread-safe)
- No external dependencies beyond solver core infrastructure
- Exception handler suppresses propagation when provided
**Scale/Scope**: Testing utility for move validation; expected usage in unit/integration tests across solver codebase and quickstarts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Real World Usefulness (Principle I)
- **Status**: PASS
- **Evidence**: 
  - Feature will be used to test moves in solver core (SC-007: "at least 2 different solver move types")
  - Dogfooding requirement ensures real-world validation
  - Testing utility provides immediate value for move implementation validation
- **Documentation Plan**:
  - Public API Javadoc for MoveRunner class and methods (MUST)
  - User guide section on testing custom moves (MUST)
  - Code examples in Javadoc showing permanent and temporary execution patterns (SHOULD)
  - Quickstart examples demonstrating move testing (external repo, SHOULD)

### ✅ Consistent Terminology (Principle II)
- **Status**: PASS
- **Evidence**:
  - "MoveRunner" consistently used throughout spec (not MoveVerifier)
  - "Temporary execution" vs "permanent execution" clear distinction
  - "Exception handler" refers to Consumer<Exception> parameter
- **Variable Naming Convention**: Will follow collection naming (e.g., `moveList`, `entitySet`, `scoreMap`)

### ✅ Fail Fast (Principle III)
- **Status**: PASS
- **Evidence**:
  - FR-001b: Null validation upfront in build() method (IllegalArgumentException for null solution class or empty entity classes)
  - FR-001d: Null validation in using() and execute() methods
  - Construction-time shadow variable initialization (FR-012b)
  - No deferred validation - all checks happen immediately
- **Implementation**: Validate inputs in static factory method `MoveRunner.build(solutionClass, entityClasses...)`, instance method `using(solution)`, and `execute()` methods

### ✅ Understandable Error Messages (Principle IV)
- **Status**: PASS (to be verified in implementation)
- **Requirements**:
  - Include variable names and states in all exceptions
  - Use String.formatted() for exception messages (JDK 13+)
  - Provide actionable advice with "maybe" prefix where appropriate
  - Example: `"The move (%s) cannot be null. Maybe you forgot to provide a move instance."`

### ✅ Automated Testing (Principle V)
- **Status**: PASS
- **Plan**:
  - Unit tests for MoveRunner API (JUnit 5 + AssertJ)
  - Integration tests with actual Move implementations
  - Test both permanent and temporary execution modes
  - Test exception handling paths (with and without handler)
  - Test resource cleanup (AutoCloseable behavior)
  - Verify shadow variable initialization
  - SC-007 ensures dogfooding with at least 2 move types

### ✅ Good Code Hygiene (Principle VI)
- **Status**: PASS
- **Compliance**:
  - Maven build will auto-format code
  - Sparse newline usage for logical blocks only
  - Field ordering consistency in implementation
  - SonarCloud quality gates enforced by CI (Reliability B+, Maintainability B+)
  - Code coverage monitored by SonarCloud

### ✅ Java Language Version (Tech Stack I)
- **Status**: PASS
- **Evidence**:
  - JDK 17 minimum (compile target)
  - Modern features applicable:
    - Records for internal data carriers (if needed)
    - Text blocks for multi-line exception messages
    - Pattern matching for instanceof
    - var keyword for obvious types
    - Switch expressions (if applicable)
  - Nullability: @NullMarked on classes, explicit @Nullable where null is part of contract

### ✅ Production Code Dependencies (Tech Stack II)
- **Status**: PASS
- **Evidence**:
  - No external dependencies beyond Java Standard Library and JSpecify
  - Only depends on solver's existing core infrastructure (ScoreDirector, Move)
  - Preview API in `ai.timefold.solver.core.preview.api.move` package

### ✅ Test Infrastructure (Tech Stack III)
- **Status**: PASS
- **Evidence**:
  - JUnit 5 for all tests (MUST)
  - AssertJ for ALL assertions (MUST - no JUnit assertions)
  - Mockito for mocking (when needed)

### ✅ Security (Tech Stack IV)
- **Status**: PASS (low security risk for testing API)
- **Evidence**:
  - No external input validation needed (programmatic API)
  - No secrets or credentials involved
  - No logging of sensitive data
  - Input validation via Fail Fast (null checks)

### ✅ Package Structure and API Stability
- **Status**: PASS
- **Evidence**:
  - Preview API: `ai.timefold.solver.core.preview.api.move` package
  - Clearly marked as Preview (subject to evolution with migration support)
  - Will include OpenRewrite migration recipes when API evolves

### ✅ Deprecation and Migration Policy
- **Status**: N/A (new feature, no deprecation)
- **Future**: When API evolves, OpenRewrite recipes will be provided

### Summary
**All constitutional gates PASS.** No violations requiring justification. Feature aligns with all core principles and technology stack requirements.

## Project Structure

### Documentation (this feature)

```text
specs/001-move-running-api/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (to be created)
├── data-model.md        # Phase 1 output (to be created)
├── quickstart.md        # Phase 1 output (to be created)
├── contracts/           # Phase 1 output (to be created - Java interfaces/API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
core/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── ai/
│   │           └── timefold/
│   │               └── solver/
│   │                   └── core/
│   │                       ├── preview/
│   │                       │   └── api/
│   │                       │       └── move/
│   │                       │           ├── package-info.java
│   │                       │           └── MoveRunner.java (public API)
│   │                       └── impl/
│   │                           └── move/
│   │                               └── [implementation classes if needed]
│   └── test/
│       └── java/
│           └── ai/
│               └── timefold/
│                   └── solver/
│                       └── core/
│                           └── preview/
│                               └── api/
│                                   └── move/
│                                       ├── MoveRunnerTest.java (unit tests)
│                                       └── MoveRunnerIT.java (integration tests)
```

**Structure Decision**: Single project structure (Option 1) selected. This is a new feature within the existing `core` module of timefold-solver. The API goes in `preview.api.move` package to signal its Preview status (subject to evolution with migration support). Implementation goes in `impl.move` package following the existing solver convention of separating public API from internal implementation.

**Package Rationale**:
- `preview.api.move` - Preview API package (subject to evolution)
- `impl.move` - Implementation package (no backward compatibility guarantees)
- This follows the existing solver pattern of `api`/`config`/`impl` separation

## Phase 0: Research & Technical Discovery

**Status**: ✅ COMPLETE

**Objective**: Resolve technical unknowns and design decisions before implementation

### Research Topics Investigated

1. **ScoreDirector Integration Pattern** - How to integrate with solver's score director infrastructure
2. **Temporary Execution and Undo Mechanism** - How to implement automatic state restoration
3. **Exception Handler Integration** - How to provide optional exception handling
4. **Resource Management and AutoCloseable** - What resources need cleanup and how
5. **Thread Safety Model** - What guarantees to provide (or not provide)
6. **API Surface Design** - Optimal fluent builder pattern design

### Decisions Made

| Decision | Choice                                                 | Rationale |
|----------|--------------------------------------------------------|-----------|
| Core Integration | Use InnerScoreDirector                                 | Reuses existing undo and shadow variable handling |
| Temporary Execution | New method on InnerScoreDirector for Consumer callback | Standard functional interface, try-finally ensures cleanup |
| Exception Handling | Method overloads with Consumer<Exception>              | Idiomatic Java, clear semantics |
| Resource Management | Delegate to ScoreDirector.close()                      | Leverages existing cleanup logic |
| Thread Safety | None (single-threaded only)                            | Matches ScoreDirector model, simpler |
| API Pattern | Static factory + instance methods                      | Fluent, type-safe, encapsulates state |

### Output Artifact

📄 **[research.md](./research.md)** - Complete technical research and design decisions

---

## Phase 1: Design & Contracts

**Status**: ✅ COMPLETE

**Objective**: Define data model, API contracts, and usage patterns

### 1. Data Model Design

**Entity**: MoveRunner<Solution_>
- Fields: scoreDirector, workingSolution
- Methods: using(), execute(), executeTemporarily(), close()
- State transitions: Not Created → Created → Closed
- Relationships: Owns ScoreDirector, operates on Solution, executes Move

**Output Artifact**: 📄 **[data-model.md](./data-model.md)**

### 2. API Contracts

**Public API Contract**: Complete method signatures with:
- Javadoc specifications
- Preconditions and postconditions
- Exception specifications with message formats
- Behavioral guarantees
- Usage patterns and anti-patterns

**Output Artifact**: 📄 **[contracts/MoveRunner-API-Contract.md](./contracts/MoveRunner-API-Contract.md)**

### 3. Developer Documentation

**Quickstart Guide**: Beginner-friendly guide covering:
- Basic permanent execution
- Temporary execution with undo
- Exception handling
- Common pitfalls and best practices
- Complete working examples

**Output Artifact**: 📄 **[quickstart.md](./quickstart.md)**

### 4. Constitution Check Re-evaluation

**Status**: ✅ ALL GATES PASS (re-verified post-design)

All constitutional principles remain satisfied after detailed design:
- Real World Usefulness: Documentation complete, dogfooding planned
- Consistent Terminology: MoveRunner used throughout
- Fail Fast: Null validation at entry points
- Understandable Error Messages: Message templates defined in contract
- Automated Testing: Test plan defined
- Good Code Hygiene: Will follow conventions
- Java Language Version: JDK 17 compatible
- Dependencies: No external dependencies
- Test Infrastructure: JUnit 5 + AssertJ
- Security: No security concerns for testing API

---

## Phase 2: Task Planning

**Status**: ⏸️ DEFERRED TO `/speckit.tasks` COMMAND

This plan document ends at Phase 1 (design). The next step is to run `/speckit.tasks` to break the implementation into specific tasks.

**Expected Task Breakdown**:
1. Implement MoveRunner public API class
2. Implement ScoreDirector integration
3. Implement permanent execution (with/without exception handler)
4. Implement temporary execution with undo
5. Implement AutoCloseable resource cleanup
6. Add comprehensive unit tests
7. Add integration tests with real moves
8. Write Javadoc for public API
9. Add package-info.java with Preview API notice
10. Update solver documentation
11. Dogfood with at least 2 move types

**To proceed**: Run `/speckit.tasks` to generate tasks.md

---

## Implementation Notes

### Critical Path

1. **Foundation**: MoveRunner class structure, factory method, ScoreDirector integration
2. **Core Features**: Permanent execution, temporary execution with undo
3. **Polish**: Exception handling, resource cleanup, error messages
4. **Validation**: Tests, documentation, dogfooding

### Dependencies

**Internal Dependencies**:
- `ScoreDirectorFactory` - Factory for creating score directors
- `InnerScoreDirector` - Implementation interface for move execution
- `Move` interface - What users execute
- Solver's undo mechanism - For temporary execution

**No External Dependencies**: Feature uses only solver core infrastructure

### Testing Strategy

**Unit Tests** (MoveRunnerTest.java):
- Factory method validation (null checks)
- Permanent execution behavior
- Temporary execution with undo
- Exception handler invocation
- Resource cleanup via AutoCloseable
- State validation (closed MoveRunner throws)

**Integration Tests** (MoveRunnerIT.java):
- Real Move implementations (SwapMove, ChangeMove, etc.)
- Shadow variable updates
- Complex undo scenarios
- Multiple sequential executions
- Dogfooding with solver's own moves

### Documentation Requirements

**Javadoc** (MUST):
- Class-level: MoveRunner purpose, Preview API status, usage example
- Method-level: All public methods with @param, @return, @throws
- Package-info.java: Preview API notice, evolution policy

**User Guide** (MUST):
- New section: "Testing Custom Moves"
- When to use MoveRunner
- Permanent vs temporary execution
- Exception handling patterns
- Try-with-resources requirement

**Code Examples** (SHOULD):
- Embedded in Javadoc
- Complete test class examples
- Anti-patterns to avoid

---

## Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Undo mechanism incomplete | High | Low | Use solver's battle-tested undo; extensive testing |
| Resource leaks if not closed | Medium | Medium | Strong documentation; consider adding finalizer warning |
| Performance overhead | Low | Low | Acceptable for testing; construction cost is one-time |
| API evolution breaks users | Medium | Medium | Preview API status; OpenRewrite migration recipes |

---

## Success Metrics

From spec.md success criteria (SC-001 through SC-007):

✅ **SC-001**: Developers can execute a custom Move and verify changes  
✅ **SC-002**: Temporary execution restores solution in 100% of normal flow cases  
✅ **SC-003**: Complete validation workflows without manual state management  
✅ **SC-004**: Workflow completes without understanding internal undo mechanisms  
✅ **SC-005**: API supports all standard Move types without type-specific handling  
✅ **SC-006**: User assertions executable during temporary scope  
✅ **SC-007**: Dogfooding with at least 2 different solver move types

**Verification**: All criteria are testable and will be verified in implementation phase.

---

## Completion Checklist

- [x] Phase 0: Research complete (research.md)
- [x] Phase 1: Data model defined (data-model.md)
- [x] Phase 1: API contracts specified (contracts/MoveRunner-API-Contract.md)
- [x] Phase 1: Quickstart guide created (quickstart.md)
- [x] Constitution Check: All gates pass
- [ ] Phase 2: Tasks broken down (run `/speckit.tasks`)
- [ ] Implementation: Code complete
- [ ] Testing: All tests pass
- [ ] Documentation: Javadoc and user guide complete
- [ ] Dogfooding: Used with 2+ move types
- [ ] Review: Code review approved
- [ ] Merge: PR merged to main

---

## Next Command

```bash
/speckit.tasks
```

This will break down the implementation into specific, trackable tasks in `tasks.md`.

---

**Plan Status**: ✅ **READY FOR TASK BREAKDOWN**  
**Branch**: `001-move-running-api`  
**Spec**: [spec.md](./spec.md)  
**Artifacts Generated**:
- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/MoveRunner-API-Contract.md](./contracts/MoveRunner-API-Contract.md)
- [quickstart.md](./quickstart.md)
- This plan.md

