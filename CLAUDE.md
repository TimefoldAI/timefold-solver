# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Timefold Solver is an AI constraint solver engine for Java/Kotlin. It is a **library** (not a standalone application) that gets embedded into Spring Boot or Quarkus apps to solve complex planning/optimization problems like vehicle routing, employee rostering, timetabling, and task scheduling.

- **License**: Apache 2.0
- **Origin**: Fork of OptaPlanner (April 2023)
- **Docs**: https://docs.timefold.ai
- **Quickstarts** (demo apps with UI): https://github.com/TimefoldAI/timefold-quickstarts
- **Constitution**: `.specify/memory/constitution.md` — the authoritative source for all coding standards and principles. When in doubt, defer to the constitution.

## Build & Run

**Requirements**: JDK 17+, Maven 3.9.11+ (wrapper included)

```bash
# Fast build (~1 min) - skips checks
./mvnw clean install -Dquickly

# Normal build (~17 min) - with tests + code style
./mvnw clean install

# Full build (~20 min) - all checks, docs, distributions
./mvnw clean install -Dfull

# Docs only (~2 min)
cd docs && ./mvnw clean install

# Build a single module (e.g. core)
./mvnw clean install -pl core -Dquickly

# Run a single test class
./mvnw test -pl core -Dtest=DefaultSolverTest

# Run a single test method
./mvnw test -pl core -Dtest=DefaultSolverTest#testMethodName

# Run integration tests for a module
./mvnw verify -pl spring-integration/spring-boot-autoconfigure
```

## Project Structure

```
core/                          # Core solver engine
  core/src/main/java/.../core/
    api/                       # Public API (100% backwards compatible)
    config/                    # Configuration classes (100% backwards compatible)
    impl/                      # Implementation (no compatibility guarantees)
    enterprise/                # Enterprise edition hooks
    preview/                   # Preview/experimental features
spring-integration/            # Spring Boot autoconfigure, starter, tests
quarkus-integration/           # Quarkus integration modules
persistence/                   # Serialization: Jackson, JAXB, JPA, JSONB
benchmark/                     # Benchmarking suite
test/                          # Test utilities and fixtures
tools/webui/                   # Embeddable Web UI components (jQuery + Bootstrap 5)
migration/                     # OpenRewrite migration recipes for API upgrades
docs/                          # AsciiDoc documentation (Antora)
build/                         # BOM, parent POM, IDE config
.specify/memory/constitution.md # Project constitution (MUST READ)
```

## Core Architecture

The solver follows a layered pipeline: **SolverFactory** creates a **Solver**, which orchestrates **Phases** (construction heuristic, local search, etc.), each executing **Steps** composed of **Moves** (atomic changes to the solution). The **ScoreDirector** incrementally tracks constraint violations as moves are applied.

Key abstractions:
- `Solver<Solution_>` — main entry point; takes a `@PlanningSolution`-annotated domain object, returns the best solution found
- `Phase` — algorithmic phase (e.g., `ConstructionHeuristicPhase`, `LocalSearchPhase`); phases run sequentially
- `Move` — atomic change to a planning variable; moves are selected by `Selector` implementations
- `ScoreDirector` — incremental score calculator; tracks score deltas without full recalculation
- `Score` — result type hierarchy (`SimpleScore`, `HardSoftScore`, `HardMediumSoftScore`, `BendableScore`, etc.)
- `Termination` — controls when the solver stops (time limit, score target, etc.)
- `SolverScope` / `PhaseScope` / `StepScope` — hierarchical state containers for solver lifecycle

Object creation pattern: users call `SolverFactory.create(SolverConfig)` → `factory.buildSolver()` → `Solver` interface. Implementation classes (`DefaultSolver`, etc.) are never directly instantiated by users.

## Key Conventions (from Constitution)

### Code Style
- **JDK 17** compile target (use modern features: var, records, text blocks, sealed classes, pattern matching)
- **No `Optional`** - use `@Nullable` from JSpecify instead
- **Streams**: use judiciously; prefer simple for-loops when clearer
- **Final by default**: classes, methods, and fields should be `final` unless designed for extension
- **Private by default**: minimize visibility
- **Variable naming**: include collection type (`tupleList`, `scoreMap`, `entitySet`, not `tuples`, `scores`, `entities`)
- **No asterisk imports** (`import java.util.*` forbidden)
- **Prefer `var`** over diamond operator
- Code is auto-formatted during Maven build (Spotless)

### Naming Conventions
- Single implementation: `Default` prefix (`DefaultSolver implements Solver`)
- Abstract classes: `Abstract` prefix (`AbstractPhase`)
- Never use `Impl` suffix
- Unit tests: `*Test.java` (Surefire)
- Integration tests: `*IT.java` (Failsafe)

### Testing
- **JUnit 5** for execution
- **AssertJ** for ALL assertions (`assertThat(...)` - JUnit assertions forbidden)
- **Mockito** for mocking (prefer real objects)
- All code MUST have tests before merge

### Commits
- **Conventional Commits**: `feat:`, `fix:`, `docs:`, `perf:`, `test:`, `build:`, `ci:`, `revert:`, `deps:`, `chore:`
- Present tense, imperative mood

### API Structure
- `api` packages: 100% backwards compatible (interfaces only)
- `config` packages: 100% backwards compatible
- Implementation packages: no compatibility guarantees
- Use factory pattern for object creation in public API

### Error Handling
- Fail fast (compile > startup > runtime > assertion mode)
- Include variable names and states in exception messages
- Offer actionable advice with "maybe" suggestions
- Use `Objects.requireNonNull()` for null checks
- Prefer unchecked exceptions

### Dependencies
- **NO external libraries in production code** (except Java stdlib, JSpecify, framework-specific modules)
- Test dependencies: JUnit, AssertJ, Mockito allowed

### Security
- No secrets in code
- Validate all external input
- Never log sensitive data or PII
- Use SLF4J for logging

## Quality Gates (CI)
- SonarCloud: Reliability >= B, Maintainability >= B
- Code coverage threshold enforced (JaCoCo, aggregated across modules)
- Spotless auto-formatting (runs during every Maven build)
- RevAPI backwards compatibility checks (baseline: 0.9.38) — changes to `api` or `config` packages are checked for binary/source compatibility
- CI tests on JDK 17, 21, and 25 across Ubuntu, macOS, and Windows

## Git Workflow
- Fork from `TimefoldAI/timefold-solver`
- `origin` = your fork (`dolverin/timefold-solver`)
- `upstream` = original (`TimefoldAI/timefold-solver`)
- Feature branches from `main`
- Build passes before PR: `./mvnw clean install`

## Exception Message Pattern

Exception messages follow a specific format throughout the codebase:
```java
throw new IllegalArgumentException(
        "The fooSize (%d) of bar (%s) must be positive."
                .formatted(fooSize, this));
// For actionable advice, add "maybe" suggestions:
throw new IllegalStateException("""
        The valueRange (%s) is nullable, but not countable (%s).
        Maybe the member (%s) should return CountableValueRange."""
        .formatted(valueRange, isCountable, member));
```
