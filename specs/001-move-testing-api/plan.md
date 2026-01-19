# Implementation Plan: Move Testing API

**Branch**: `001-move-testing-api` | **Date**: January 19, 2026 | **Spec**: [spec.md](../../specs/001-move-testing-api/spec.md)
**Input**: Feature specification from `/specs/001-move-testing-api/spec.md`

**Note**: This implementation plan was generated following the speckit.plan workflow.

## Summary

This feature provides an API to verify implementations of the Move interface. The API allows developers to execute moves on planning solutions or individual entities, with support for temporary execution (with automatic undo). The primary approach leverages the solver's existing undo mechanisms via `EphemeralMoveDirector` to ensure reliable state restoration without reimplementing complex state management logic.

## Technical Context

**Language/Version**: Java 17 (compile target), supports runtime up to latest JDK  
**Primary Dependencies**: JSpecify (compile-time nullability annotations only); Constraint Streams for internal ScoreDirectorFactory creation  
**Storage**: N/A (in-memory testing API)  
**Testing**: JUnit 5, AssertJ (mandatory per constitution)  
**Target Platform**: JVM (cross-platform)  
**Project Type**: Library module (test utilities)  
**Performance Goals**: Minimal overhead for test execution; leverages solver's existing undo mechanisms to avoid performance penalties  
**Constraints**: Zero production dependencies (test module only); must not interfere with solver's subsystems (listeners, shadow variables)  
**Scale/Scope**: Small focused API (1 public class with 2 factory methods and 2 execution methods) for testing move implementations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Initial Check (Pre-Phase 0)

#### Core Principles Compliance

- ✅ **Real World Usefulness**: API will be validated with test examples covering standard Move implementations
- ✅ **Consistent Terminology**: Uses established solver terms (Move, Planning Solution, Planning Entity, Planning Variable, Shadow Variable)
- ✅ **Fail Fast**: Validates null inputs upfront (FR-001b); leverages solver's existing validation for invalid states
- ✅ **Understandable Error Messages**: Exception messages will include variable names, states, and actionable advice per constitution standards
- ✅ **Automated Testing**: Full unit and integration test coverage required; tests use JUnit 5 + AssertJ
- ✅ **Good Code Hygiene**: Code will follow standard Java conventions; automatic formatting via Maven build

#### Technology Stack Compliance

- ✅ **Java Language Version**: Targets JDK 17 compile-time, supports latest JDK runtime
- ✅ **Production Dependencies**: Zero external dependencies (test module uses only JSpecify annotations)
- ✅ **Test Infrastructure**: Uses JUnit 5 and AssertJ as mandated
- ✅ **Security**: No secrets, no external input, no security risks (internal testing API)
- ✅ **Logging**: Will use SLF4J if logging needed; no sensitive data logged

#### Package Structure Compliance

- ✅ **Public API**: New API classes will be in `ai.timefold.solver.test.api.move` package (100% backwards compatible)
- ✅ **Implementation**: Implementation classes in `ai.timefold.solver.test.impl.move` (no compatibility guarantees)
- ✅ **Documentation**: Javadoc required on all public APIs with @param, @return, @throws tags
- ✅ **No @since tags**: Per constitution, @since tags are not required

#### Quality Gates

- ✅ **SonarCloud**: Must maintain Reliability and Maintainability grades B or better
- ✅ **Code Coverage**: Must meet or exceed configured threshold
- ✅ **Nullability**: Classes will use @NullMarked; null confined to implementation internals

**GATE STATUS (PRE-PHASE 0)**: ✅ PASSED - No constitutional violations; ready for Phase 0 research

### Post-Phase 1 Re-check

All constitution checks remain PASSED after completing design phase. The detailed API design in data-model.md and contracts confirms:

- ✅ **Real World Usefulness**: Quickstart examples demonstrate practical usage in 10 realistic test scenarios
- ✅ **API Design**: Maximally simplified API with 1 public class (MoveTester) using standard Java `Function<Solution_, Result_>`
- ✅ **Implementation Strategy**: Leverages existing solver infrastructure (EphemeralMoveDirector, VariableChangeRecordingScoreDirector); creates ScoreDirectorFactory automatically
- ✅ **Documentation Complete**: Full Javadoc structure defined, usage examples provided, API contracts documented
- ✅ **No New Dependencies**: Confirmed zero production dependencies beyond JSpecify annotations
- ✅ **Simplified Design**: No custom interfaces; uses standard `Function` for temporary execution; standard try-catch for exception handling; automatic ScoreDirectorFactory creation

**GATE STATUS (POST-PHASE 1)**: ✅ PASSED - Design validated against constitution

## Project Structure

### Documentation (this feature)

```text
specs/001-move-testing-api/
├── plan.md              # This file - implementation plan
├── research.md          # Phase 0 output - solver internals research
├── data-model.md        # Phase 1 output - API class structure
├── quickstart.md        # Phase 1 output - usage examples
└── contracts/           # Phase 1 output - API contracts
    └── move-testing-api.md  # Public API specification
```

### Source Code (repository root)

```text
test/
├── src/
│   ├── main/java/ai/timefold/solver/test/
│   │   ├── api/
│   │   │   └── move/               # NEW: Public API for move testing
│   │   │       └── MoveTester.java              # Main API class (factory + execution)
│   │   └── impl/
│   │       └── move/               # NEW: Implementation details
│   │           ├── MoveTestSession.java          # Internal session management
│   │           ├── TestingSolution.java            # Built-in generic solution class
│   │           └── TestingSolutionFactory.java     # Creates dummy solution instances
│   └── test/java/ai/timefold/solver/test/
│       └── api/
│           └── move/               # NEW: Tests for the testing API
│               ├── MoveTesterTest.java
│               └── TestingSolutionFactoryTest.java
```

**Structure Decision**: This is a single-module addition to the existing `test/` module. The feature adds a new package `ai.timefold.solver.test.api.move` with a single public API class `MoveTester` (stable with backwards compatibility) and `ai.timefold.solver.test.impl.move` for implementation details (may change). This follows Timefold's established package structure conventions where `api` packages have stability guarantees and `impl` packages do not. The API has been maximally simplified: MoveTester combines both factory methods and execution methods, uses standard `java.util.function.Function<Solution_, Result_>` for temporary execution (no custom interface), and relies on standard Java try-catch for exception handling (no special exception handling API). For testing without a full solution, the API provides a built-in `TestingSolution` class, eliminating the need for users to provide their own solution class.

## Complexity Tracking

No constitutional violations identified - this section is not needed.
