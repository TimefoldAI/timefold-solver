# Timefold Solver Constitution

## Terminology

Key words per [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119.html): **MUST** = absolute requirement; **MUST NOT** = absolute prohibition; **SHOULD** = strong recommendation; **SHOULD NOT** = strong recommendation against; **MAY** = optional.

## Core Principles

### I. Real World Usefulness

- Every suitable feature MUST be used in at least one quickstart
- Features considered complete only when: fully tested, fully documented, validated in realistic scenario

**Documentation**:

1. **Public API** (MUST): All public classes/interfaces/methods MUST have Javadoc with `@param`, `@return`, `@throws` (omit NPE for null params; omit if present on referenced overload). `@since` optional.
2. **User-facing** (MUST): 
   - New features → user guide; 
   - Config changes → reference docs; 
   - Breaking changes → migration guides.
3. **Examples** (SHOULD): Docs SHOULD include code examples; quickstart examples SHOULD be added externally.
4. **Implementation** (SHOULD): Complex classes SHOULD have class-level Javadoc; non-obvious details SHOULD have comments.

### II. Consistent Terminology

Names MUST be unambiguous and consistent across codebase, docs, and public communication.

**Collections**: include data structure type in variable name:
- ✅ `tupleList`, `scoreMap`, `entitySet`, `problemArray`
- ❌ `tuples`, `scores`, `entities`, `problems`

**Interfaces and Implementations**:

1. **Single implementation** (MUST): prefix with `Default`
   - ✅ `interface Solver` → `class DefaultSolver`; ❌ `SolverImpl`
2. **Multiple implementations** (SHOULD): descriptive names; `Default` for the most common
3. **Abstract classes** (MUST): prefix with `Abstract`; never `Base`
   - ✅ `abstract class AbstractPhase`; ❌ `abstract class BasePhase`

### III. Fail Fast

Validate invalid states ASAP, in priority order:

1. **Compile time** — type safety
2. **Startup time** — config validation
3. **Runtime** — request parameter validation (see also: Security § Input Validation)
4. **Assertion mode** — performance-sensitive checks gated on `EnvironmentMode`

### IV. Understandable Error Messages

Exception messages MUST include variable names/states:
```java
throw new IllegalArgumentException(
        "The fooSize (%d) of bar (%s) must be positive.".formatted(fooSize, this));
```

Actionable advice SHOULD be on new line starting with "Maybe":
```java
throw new IllegalStateException("""
        The valueRangeDescriptor (%s) is nullable, but not countable (%s).
        Maybe the member (%s) should return CountableValueRange."""
        .formatted(valueRangeDescriptor, isCountable, member));
```

**Exception Handling**:
- Prefer unchecked exceptions for programming errors
- Never swallow exceptions; always handle or rethrow with context
- Don't catch `Exception`/`Throwable`
- Null params → `Objects.requireNonNull(param, "param")`

### V. Automated Testing

All code MUST have tests before merging. Methodology (TDD etc.) is discretionary.

**Naming** (required for Maven Surefire/Failsafe):
- Unit tests: `*Test` (e.g., `ScoreCalculatorTest`) — Surefire, `test` phase
- Integration tests: `*IT` (e.g., `SolverEndToEndIT`) — Failsafe, `integration-test` phase

**Classpath** (MUST): Tests MUST run on classpath, NOT modulepath. No `module-info.java` in test roots.

Performance/stress tests are in other repositories; not applicable here.

### VI. Good Code Hygiene

- Code MUST be clean and readable; **readability over ease of writing**
- `./mvnw clean install` auto-formats; CI enforces style

**Style**:
1. Newlines: formatter preserves them; use sparingly to separate logical blocks
2. Field access order in methods SHOULD match declaration order
3. Imports MUST be used; no fully qualified names in source (exception: disambiguating same simple name)
4. `var` preferred over diamond; diamond MUST be used when `var` not used and type is inferable
   - ✅ `var list = new ArrayList<String>();`; ✅ `List<String> list = new ArrayList<>();`
   - ❌ `var list = new ArrayList<>();` (compiler error); ❌ `List<String> list = new ArrayList<String>();`
5. Asterisk imports MUST NOT be used

**SonarCloud Quality Gates** (MUST):
- Reliability and Maintainability grades MUST be B or better; PRs worsening below B fail CI
- Coverage MUST not fall below configured threshold
- Critical/major code smells SHOULD be resolved before merge

**Visibility and Immutability** (MUST):
- Fields/methods MUST be `private` unless broader visibility required
- Classes/methods MUST be `final` unless designed for extension
- Fields MUST be `final` unless value must change post-construction
- Prefer immutable objects: `List.of()`, `Set.of()`, records
- Seal interfaces/abstract classes if no external implementations allowed

```java
// ✅ Correct
public final class DefaultSolver<Solution_> implements Solver<Solution_> {
    private final SolverScope<Solution_> solverScope;
    DefaultSolver(SolverScope<Solution_> solverScope) { // package-private
        this.solverScope = Objects.requireNonNull(solverScope);
    }
    @Override
    public final Solution_ solve(Solution_ problem) { ... }
}
```

## Technology Stack and Dependency Constraints

### I. Java Language Version

- Minimum: JDK 21 (compile and runtime)
- SHOULD support latest JDK runtime
- All JDK 21 features encouraged
- `Optional` MUST NOT be used; use `@Nullable` (JSpecify) instead
- Streams allowed but prefer simple `for` loops in hot paths

**Nullability (JSpecify)**:
- Use `@NullMarked` on classes/packages → everything non-null by default
- Internal null is permitted
- Null MUST NOT escape public APIs without `@Nullable`
- `@Nullable` required on any public method accepting/returning null

```java
@NullMarked
public class ScoreCalculator {
    private Solution cachedSolution = null; // internal null OK
    public Solution calculate(Problem problem) { ... } // non-null by @NullMarked
    public @Nullable Solution getCachedSolution() { return cachedSolution; }
}
```

**Enforcement**: Build targets JDK 21; CI verifies compilation and tests on JDK 21; code reviews MUST reject `Optional` and null escaping APIs.

### II. Production Code Dependencies

**No external libraries in production code** except:
1. Java Standard Library
2. JSpecify annotations (compile-time only)
3. Module-specific frameworks (Quarkus in Quarkus modules, Spring in Spring modules only)
4. Explicitly ratified exceptions in this constitution

Code reviews MUST reject unauthorized production dependencies.

### III. Test Infrastructure

MUST use:
1. **JUnit** — test execution, lifecycle annotations
2. **AssertJ** (MUST) — `assertThat(actual).isEqualTo(expected)`; JUnit assertions (`assertEquals` etc.) FORBIDDEN
3. **Mockito** — mocking/stubbing; prefer real objects when practical
4. Other frameworks allowed when they provide clear value (test containers, property-based testing, etc.)

### IV. Security

1. **No Secrets** (MUST NOT): No credentials/keys/tokens in code; use env vars or config files excluded from VCS
2. **Dependency Security** (MUST): High-severity CVEs MUST be addressed promptly; Dependabot provides weekly upgrade PRs; Aikido performs security scanning
3. **Input Validation** (MUST): All external input MUST be validated; assume malicious
4. **Secure Defaults** (MUST): Insecure options require explicit opt-in; document with warnings
5. **Vulnerability Reporting** (MUST): Report privately; do NOT create public issues
6. **No sensitive logging** (MUST NOT): No credentials, PII in logs

**Logging (SLF4J)**:
- ERROR: unrecoverable failures; WARN: recoverable issues/deprecations; INFO: solver lifecycle; DEBUG: phase lifecycle; TRACE: step lifecycle + developer debugging
- No sensitive data; minimize in hot paths

## Package Structure and API Stability

| Package type | Stability |
|---|---|
| `*.api.*` | 100% backwards compatible; breaking only in major versions |
| `*.config.*` | 100% backwards compatible; breaking only in major versions |
| All others | No guarantees |

**Versioning**: MAJOR = breaking API/config change; MINOR = new backwards-compat feature; PATCH = bug fix.

**API Design Principles** (MUST):
1. Public API packages MUST expose interfaces, not implementations
2. Implementation constructors MUST be hidden (package-private); use factories/builders
3. Object creation in public API MUST use factory pattern
4. Public API methods MUST return interface types, not implementation types

```java
// ✅ Correct
package ai.timefold.solver.core.api.solver;
public interface SolverFactory<Solution_> {
    static <Solution_> SolverFactory<Solution_> create(SolverConfig config) {
        return new DefaultSolverFactory<>(config);
    }
    Solver<Solution_> buildSolver();
}

public final class DefaultSolver<Solution_> implements Solver<Solution_> {
    DefaultSolver(SolverScope<Solution_> solverScope) { ... } // package-private
}
```

### JPMS

JPMS MUST be implemented; every generated jar MUST have `module-info.java`.

- Directives (`exports`, `opens`, `provides`, `requires`, `uses`) MUST be alphabetically sorted.
- `.api.*` and `.config.*` packages MUST be exported.
- `.impl.*` packages MUST NOT be exported; if unavoidable, use qualified exports (`exports pkg to module;`).
- Modules MUST NOT be fully `open`; only open specific packages to specific modules.
- Our modules SHOULD `requires transitive` our other modules where possible; external modules MUST NOT be `transitive`.

```java
module ai.timefold.solver.jaxb {

    exports ai.timefold.solver.jaxb.api.score;

    // ai.timefold.solver.core is a module of ours, SHOULD use transitive
    requires transitive ai.timefold.solver.core;
    // jakarta.xml.bind is not a module of ours, MUST NOT be transitive
    requires jakarta.xml.bind;

}
```

## Development Workflow

### Contribution Process

1. Discuss significant changes in GitHub Discussions/Issues first
2. Feature branch from main
3. Conventional commits
4. `./mvnw clean install` — all checks pass
5. Submit PR; CI validates; maintainers review

### Commit Message Format

[Conventional Commits](https://www.conventionalcommits.org/): `<type>: <description>`

Types: `feat`, `fix`, `docs`, `perf`, `test`, `build`, `ci`, `revert`, `deps`, `chore`

Present tense, imperative mood.

### Build Commands

- **Fast**: `./mvnw clean install -Dquickly` — skips checks (~1 min)
- **Normal**: `./mvnw clean install` — tests and checks (~17 min)
- **Docs**: `./mvnw clean install` in `docs/` (~2 min)
- **Full**: `./mvnw clean install -Dfull` — all checks + distribution (~20 min)

### Deprecation and Migration Policy

1. **Mark** (MUST): `@Deprecated` annotation + `@deprecated` Javadoc with explanation, alternative, and version info
2. **Migration path** (MUST): explain why deprecated, provide alternative, code example, removal timeline
3. **OpenRewrite recipe** (SHOULD): automate migration; skip only if technically infeasible
4. Deprecated features MUST NOT be removed without at least one major version notice period

```java
/** @deprecated Use {@link #calculateScoreOptimized(Solution)} instead. Removed in 2.0.0. */
@Deprecated(since = "1.8.0", forRemoval = true)
public Score calculateScore(Solution solution) { ... }
```

## Governance

### Intellectual Property and Licensing

1. All contributions MUST be Apache 2.0 compatible
2. Third-party code MUST include attribution; original copyright MUST be preserved
3. License headers in source files NOT required
4. Contributors MUST have rights to contribute the code

### Constitution Authority

This constitution supersedes all other development practices. All PRs MUST verify constitutional compliance.

Constitution amendments require maintainer discussion and approval. 
Constitution versioning: MAJOR = principle removal/redefinition; MINOR = new principles; PATCH = clarifications.

### Compliance

- CI enforces automated checks (commit format, style, coverage)
- MUST violations block PR approval
- Deviations MUST be explicitly justified

---

**Version**: 2.1.0
