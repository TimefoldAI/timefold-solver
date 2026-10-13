# AGENTS.md

Timefold Solver: an AI constraint solver for Java and Kotlin.
For the project overview, see [README.md](README.md).

## Rules

[CONSTITUTION.md](CONSTITUTION.md) is the authority. Read it before you change code.
It defines naming, nullability, error messages, visibility, dependencies, tests, JPMS, API stability, deprecation, and commits.
If a rule here conflicts with the constitution, the constitution wins.

## Agent-specific instructions

- You must talk concisely and in ASD-STE100 Simplified Technical English.
- Do not add yourself as a co-author to the commit.
- Do not create public issues for security problems.
- Before you edit a package, find its stability level in the constitution (*Package Structure and API Stability*).
  A change to `*.api.*` or `*.config.*` must stay backwards compatible. A change to `*.internal.*` should stay backwards compatible unless there is no other option.
- Do not add production dependencies. Ask first.
- Write a test for each change, in a red-green fashion. Use AssertJ only.
- Keep comments short. Comments describe the current code, not its history. History goes in the commit message.
- Do not format code by hand. The Maven build formats it; `spotless:apply` alone is not sufficient.
- A new or changed feature needs documentation in `docs/src/modules/ROOT/pages/`.
  A breaking change needs a migration guide in `docs/src/modules/ROOT/pages/upgrading-timefold-solver/`,
  and, if possible, an OpenRewrite recipe in `tools/migration/`.

## Modules

| Directory | Contents |
|---|---|
| `core/` | The solver |
| `persistence/` | JAXB, Jackson, JPA integration |
| `spring-integration/` | Spring Boot integration; only Spring code goes here |
| `quarkus-integration/` | Quarkus integration; only Quarkus code goes here |
| `service/` | Service model building blocks |
| `tools/` | Benchmark, benchmark aggregator, OpenRewrite migration |
| `docs/` | User guide (Antora, AsciiDoc) |
| `build/` | BOM, build parent, IDE configuration |

## Build and test

See *Build the Timefold Solver Project* in [CONTRIBUTING.md](CONTRIBUTING.md#build-the-timefold-solver-project).
For a quick loop on one module:

```bash
./mvnw clean install -Dquickly                  # once, to install all modules
./mvnw -pl core test -Dtest=SomeClassTest       # one test class
```
