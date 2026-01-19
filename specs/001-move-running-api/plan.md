# Implementation Plan: Move Running API

**Branch**: `001-move-running-api` | **Date**: January 19, 2026 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-move-running-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

The Move Running API provides a testing utility for developers to execute Move implementations on planning solutions in both permanent and temporary (with automatic undo) modes. The API uses a builder pattern (`MoveRunner.build(solutionClass, entityClasses...).using(solution).execute(move)`) and is explicitly designed for testing scenarios, not production workflows. It leverages the solver's existing undo mechanisms for state restoration and requires try-with-resources pattern for proper resource cleanup.

## Technical Context

**Language/Version**: Java 17 (compile-time compatibility, supports latest JDK runtime)  
**Primary Dependencies**: JSpecify (nullability annotations), solver core internals (score director, solution descriptor, variable listeners)  
**Storage**: N/A (in-memory solution state management)  
**Testing**: JUnit 5 + AssertJ (mandatory per constitution), Mockito (optional for mocking)  
**Target Platform**: JVM (any platform supporting JDK 17+)  
**Project Type**: Single library project (core solver module)  
**Performance Goals**: Not performance-critical (testing API), but O(changes) for execution and undo operations  
**Constraints**: Single-threaded use only, must not interfere with solver's existing mechanisms (listeners, shadow variables), undefined behavior on exceptions during temporary execution  
**Scale/Scope**: API surface consists of 2-3 classes (MoveRunner, execution context, possibly exception handling), testing-focused utility

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Core Principles Review

✅ **Real World Usefulness** (MUST)
- API will be dogfooded in solver's own tests (SC-007: at least 2 move types)
- Feature includes comprehensive testing requirements (unit, integration)
- Documentation requirements addressed in Phase 1 (API docs, user guide)

✅ **Consistent Terminology** (MUST)
- Feature spec uses "Move", "MoveRunner", "Planning Solution", "Planning Entity" consistently
- Variable naming will follow collection-type convention (e.g., `entityClassList`, `moveList`)

✅ **Fail Fast** (MUST)
- Null validation at builder entry points (FR-001b, FR-001d)
- IllegalArgumentException for invalid inputs at construction time
- IllegalStateException for closed MoveRunner usage
- Early validation of solution/entity classes during MoveRunner construction

✅ **Understandable Error Messages** (MUST)
- Exception messages will include variable names and states
- Provide actionable advice where appropriate (e.g., "maybe you forgot to close the MoveRunner")

✅ **Automated Testing** (MUST)
- Unit tests for each API method and edge case
- Integration tests for move execution and undo mechanisms
- Test naming: `*Test.java` for unit tests, `*IT.java` for integration tests

✅ **Good Code Hygiene** (MUST)
- Auto-formatted via Maven build
- Use `@NullMarked` on all classes, explicit `@Nullable` where needed
- Prefer `var` for local variables, diamond operator for generic types
- Follow import conventions (no fully qualified names except for conflicts)

### Technology Stack Review

✅ **Java Language Version** (MUST)
- Target JDK 17 compile-time compatibility
- Use modern Java features: records (if applicable), text blocks for error messages, pattern matching, var keyword

✅ **Production Dependencies** (MUST)
- **Zero new external dependencies** - uses only solver core internals and JSpecify
- No unauthorized libraries introduced

✅ **Test Infrastructure** (MUST)
- JUnit 5 for test execution
- AssertJ for all assertions (no JUnit assertions)
- Mockito allowed if needed for isolation

✅ **Security** (MUST)
- No secrets/credentials involved
- Input validation for null checks (NullPointerException for nulls)
- No logging of sensitive data (not applicable to this feature)

### API Stability Review

✅ **Preview API** (Package Structure)
- Feature spec indicates "Preview" status with migration support via OpenRewrite
- API will be placed in `ai.timefold.solver.core.preview.api.move` package
- Package name includes `api` → public API with backwards compatibility expectations
- Package name includes `preview` → signals evolving API with migration support
- Semantic versioning: breaking changes allowed with OpenRewrite recipes

### Documentation Requirements

✅ **Public API Documentation** (MUST)
- All public classes/methods require Javadoc
- Include purpose, @param, @return, @throws (except NullPointerException for nulls)
- Complex algorithms documented with implementation comments

✅ **User-Facing Documentation** (MUST)
- User guide update for new testing API
- Migration guide if deprecating any existing functionality (N/A for new feature)

✅ **Examples** (SHOULD)
- Code examples in Javadoc and user guide
- Quickstart example demonstrating usage (external repository)

### Logging Requirements

✅ **Logging Policy** (MUST)
- Use SLF4J API for any logging (if needed)
- FR-015: "System MUST NOT add API-specific logging" - rely on solver's existing mechanisms
- No performance-sensitive logging in hot paths

### Assessment

**Status**: ✅ **PASS** - All constitutional gates satisfied

No violations to track in Complexity Tracking section.

## Project Structure

### Documentation (this feature)

```text
specs/001-move-running-api/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── api-contract.md  # API interface definitions
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
core/src/main/java/ai/timefold/solver/core/preview/api/move/
├── MoveRunner.java           # Main entry point: build() factory, AutoCloseable
├── MoveRunContext.java       # Execution context: using(solution) result
└── package-info.java            # Package documentation

core/src/main/java/ai/timefold/solver/core/impl/move/
├── DefaultMoveRunner.java       # Implementation of MoveRunner
└── DefaultMoveRunContext.java   # Implementation of execution context

core/src/test/java/ai/timefold/solver/core/preview/api/move/
├── MoveRunnerTest.java       # Unit tests for MoveRunner API
└── MoveRunnerIT.java         # Integration tests for end-to-end execution

docs/src/modules/ROOT/pages/optimization-algorithms/
└── neighborhoods.adoc        # relevant user guide to be updated with API documentation
```

**Structure Decision**: Single project structure (Option 1) applies. This is a core solver library feature with:
- Public API in `preview.api.move` package (preview status, subject to evolution)
- Implementation in `impl.move` package (no backwards compatibility guarantees)
- Tests colocated with production code in `core/src/test/java`
- Documentation updates to existing user guide

**Package Placement Rationale**:
- `ai.timefold.solver.core.preview.api.move`: Signals preview API status while maintaining public API contract
- `ai.timefold.solver.core.impl.move`: Follows solver's existing impl package structure for implementations (alongside existing move-related implementation classes)

## Phase 0 Completion

✅ **Phase 0: Outline & Research** - COMPLETE

All research topics resolved and documented in [research.md](research.md):
- Solver's undo mechanisms (leverage MutableSolutionView)
- Score director and solution descriptor construction
- Resource management and lifecycle (AutoCloseable pattern)
- Exception handling strategy (Consumer<Exception> with suppression)
- Thread safety and concurrency (not thread-safe by design)
- Builder pattern API design (build → using → execute)
- Shadow variable initialization (automatic at construction)
- Nested temporary execution (documented, not enforced)
- Solution state modification in callbacks (documented, not enforced)

## Phase 1 Completion

✅ **Phase 1: Design & Contracts** - COMPLETE

Generated artifacts:
- [data-model.md](data-model.md): Core API classes (MoveRunner, MoveRunContext)
- [contracts/api-contract.md](contracts/api-contract.md): Public API specifications
- [quickstart.md](quickstart.md): Usage examples and best practices

### Constitution Check Re-evaluation (Post-Design)

All constitutional requirements remain satisfied after design phase:

✅ **Documentation Requirements**:
- API contracts fully documented with Javadoc-style comments
- Quickstart guide provides usage examples and best practices
- Data model documents class structure and relationships
- User guide updates planned (not part of this plan phase)

✅ **API Design Compliance**:
- Uses builder pattern for fluent API (FR-001c)
- Implements fail-fast validation at build() and using() methods
- Clear error messages with variable names and actionable advice
- Leverages existing solver infrastructure (no new dependencies)

✅ **Code Quality Expectations**:
- Design uses modern Java features (var, text blocks for errors, pattern matching)
- @NullMarked on all classes with explicit @Nullable where needed
- Follows solver's existing package structure conventions

✅ **Testing Strategy**:
- Unit tests planned for all public methods and edge cases
- Integration tests planned for end-to-end move execution
- Test naming conventions: *Test.java for unit, *IT.java for integration

**Final Assessment**: ✅ **PASS** - Design phase complete, ready for implementation (Phase 2)

## Next Steps

The following phase is NOT executed by this command:

**Phase 2: Task Breakdown** - Execute via `/speckit.tasks` command
- Break down implementation into discrete tasks
- Assign priorities and dependencies
- Generate tasks.md file

