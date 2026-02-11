# Timefold Solver Constitution

## Terminology

This constitution uses the key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" as defined in [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119.html).

In summary:
- **MUST** / **REQUIRED** / **SHALL**: Absolute requirement
- **MUST NOT** / **SHALL NOT**: Absolute prohibition
- **SHOULD** / **RECOMMENDED**: Strong recommendation, may be ignored only with valid justification
- **SHOULD NOT**: Strong recommendation against, may be done only with valid justification
- **MAY** / **OPTIONAL**: Truly optional, discretionary

## Core Principles

### I. Real World Usefulness

Every feature MUST demonstrate real-world value before delivery:

- Every suitable feature MUST be used in at least one quickstart
- Features are only considered complete when they are:
  - Fully tested (unit, integration, and where applicable, performance tests)
  - Fully documented (API docs, user guides, examples)
  - Validated in a realistic scenario

**Documentation Standards**:

Documentation is a first-class requirement for all features:

1. **Public API Documentation** (MUST):
   - All public classes, interfaces, and methods MUST have Javadoc
   - Javadoc SHOULD include: purpose, parameters (with `@param`), return values (with `@return`), exceptions thrown (with `@throws`) except for `NullPointerException` for null parameters;
     these tags can be omitted if they are already present on another method overload
     and that overload is referenced in this method's documentation
   - Complex algorithms SHOULD include implementation comments explaining the approach
   - **Note**: `@since` tags are OPTIONAL

2. **User-Facing Documentation** (MUST):
   - New features visible to users MUST have user guide updates
   - Configuration changes MUST be documented in reference documentation
   - Breaking changes MUST be documented in migration guides

3. **Examples and Code Samples** (SHOULD):
   - Documentation SHOULD include code examples demonstrating usage
   - **Note**: Quickstarts are maintained in an external repository and are not part of this codebase, but features SHOULD have corresponding quickstart examples added there

4. **Implementation Documentation** (MAY):
   - Complex implementation classes SHOULD have class-level Javadoc explaining their role
   - Non-obvious implementation details SHOULD be explained with comments
   - Package-level documentation (package-info.java) is welcome but NOT required

**Rationale**: Features without real-world validation often suffer from usability issues, incomplete requirements, or misaligned design. Examples serve as both validation and living documentation. Documentation is how users discover, understand, and correctly use features - undocumented code is effectively unusable.

### II. Consistent Terminology

Names of features, components, and concepts MUST be unambiguous and used consistently throughout:

- The codebase (implementation, tests, comments)
- Documentation (user guides, API docs, tutorials)
- Public communication (issues, discussions, release notes)

**Variable Naming Convention**:

When a variable is a collection or a map, include that type in the variable name:

- ✅ Use `tupleList` instead of `tuples` for a List
- ✅ Use `scoreMap` instead of `scores` for a Map
- ✅ Use `entitySet` instead of `entities` for a Set
- ✅ Use `problemArray` instead of `problems` for an array

This convention makes the data structure immediately clear without needing to inspect the variable declaration, reducing cognitive load during code review and maintenance.

**Rationale**: Consistent terminology reduces cognitive load, prevents misunderstandings, and creates a coherent mental model for users and contributors. Clear variable names that include their data structure type make code self-documenting and easier to understand at a glance.

**Naming Conventions for Interfaces and Implementations**:

When defining interfaces and their implementations, follow these naming rules:

1. **Single Implementation Pattern** (MUST):
   - When an interface has only one implementation, especially if the interface is part of the public API, the implementation class MUST be prefixed with "Default"
   - ✅ `interface Solver` → `class DefaultSolver implements Solver`
   - ✅ `interface ScoreCalculator` → `class DefaultScoreCalculator implements ScoreCalculator`
   - ❌ `interface Solver` → `class SolverImpl implements Solver` (incorrect - do not use "Impl" suffix)
   - This pattern clearly indicates that the class is the standard/default implementation of the interface

2. **Multiple Implementation Pattern** (SHOULD):
   - When an interface has multiple implementations, each implementation SHOULD have a descriptive name reflecting its specific behavior or strategy
   - ✅ `interface Phase` → `class ConstructionHeuristicPhase implements Phase`, `class LocalSearchPhase implements Phase`
   - ✅ `interface Selector` → `class RandomSelector implements Selector`, `class CachingSelector implements Selector`
   - The "Default" prefix SHOULD be used for the most common or standard implementation if one exists

3. **Abstract Classes** (MUST):
   - Abstract classes MUST be prefixed with "Abstract"
   - ✅ `abstract class AbstractPhase implements Phase`
   - ✅ `abstract class AbstractSelector implements Selector`
   - ❌ `abstract class BasePhase implements Phase` (incorrect - use "Abstract" not "Base")
   - This makes the abstract nature of the class immediately visible without needing to inspect the class definition

**Rationale**: Consistent naming conventions for interfaces and implementations improve code discoverability and maintainability. The "Default" prefix clearly identifies standard implementations, making it easier for developers to find the primary implementation of an interface. The "Abstract" prefix immediately signals that a class cannot be instantiated directly and serves as a base for concrete implementations. Avoiding the "Impl" suffix prevents generic, uninformative names that provide no semantic value. These conventions align with common Java patterns and reduce cognitive load when navigating the codebase.

### III. Fail Fast

Invalid states MUST be checked as early as possible, in the following priority order:

1. **Fail Fast at compile time** - Preferred. Do not accept an `Object` as a parameter if it needs to be `String` or an `Integer`. Use type safety to prevent errors before runtime.

2. **Fail Fast at startup time** - If configuration parameters can be validated (e.g., a positive `int` that is negative), fail immediately during initialization.

3. **Fail Fast at runtime** - Validate request parameters and invariants as soon as they are received. For example, if a request requires a double between `0.0` and `1.0`, reject values outside this range immediately.
   
   **See also**: Technology Stack Principle IV for security-related input validation

4. **Fail Fast in assertion mode** - For performance-sensitive checks (e.g., verifying that variable A equals the square root of B after every iteration), perform validation only when assertion flags are enabled, typically controlled by the EnvironmentMode.

**Rationale**: Early detection prevents cascading failures, reduces debugging time, and provides clear feedback at the point of error rather than downstream.

### IV. Understandable Error Messages

All exception messages MUST be clear, actionable, and informative:

1. **Include variable names and states** - Exception messages must show the name and current state of each relevant variable:
   ```java
   if (fooSize < 0) {
       throw new IllegalArgumentException(
               "The fooSize (%d) of bar (%s) must be positive.".formatted(fooSize, this));
   }
   ```
   This produces: `IllegalArgumentException: The fooSize (-5) of bar (myBar) must be positive.`

2. **Provide context** - Whenever possible, include information about what operation was being performed and why it failed.

3. **Offer actionable advice** - When the fix is not obvious, include suggestions starting with "maybe" on a new line:
   ```java
   throw new IllegalStateException("""
           The valueRangeDescriptor (%s) is nullable, but not countable (%s).
           Maybe the member (%s) should return CountableValueRange."""
           .formatted(valueRangeDescriptor, isCountable, member));
   ```
   This produces:
   ```
   Exception in thread "main" java.lang.IllegalStateException: The valueRangeDescriptor (fooRange) is nullable, but not countable (false).
   Maybe the member (getFooRange) should return CountableValueRange.
       at ...
   ```
   The word "maybe" indicates the advice may not apply to all cases but provides a starting point for resolution.

**Exception Handling**:

Proper exception handling is essential for maintainable and debuggable code:

1. **Prefer unchecked exceptions** - Use unchecked exceptions (RuntimeException subclasses) for programming errors and precondition violations
2. **Use checked exceptions sparingly** - Only for truly recoverable conditions where the caller must handle the situation
3. **Never swallow exceptions** - Always handle exceptions meaningfully or at minimum log them before rethrowing
4. **Preserve exception context** - When wrapping exceptions, include the original cause: `throw new CustomException("Additional context", originalException)`
5. **Don't catch Exception or Throwable** - Catch specific exception types; catching Exception/Throwable masks errors like OutOfMemoryError
6. **Use `NullPointerException` appropriately** - Throw NPE when a null parameter is passed to a method that does not accept null. You SHOULD use `Objects.requireNonNull(param, "param")` for clarity.

**Rationale**: Developers spend significant time debugging. Clear error messages reduce support burden and accelerate problem resolution. Proper exception handling ensures errors are visible and debuggable rather than silently lost.

### V. Automated Testing

High-quality automated testing is REQUIRED across all levels:

- **Unit tests** - Test individual components in isolation
- **Integration tests** - Test component interactions and contracts
- **High test coverage** - Aim for comprehensive coverage of critical paths

All code MUST have tests before being merged. The methodology used to develop the code (test-first, test-after, etc.) is left to the discretion of the contributor.

**Test Naming Conventions**:

Test classes MUST follow these naming conventions to be properly picked up by Maven Surefire and Failsafe plugins:

1. **Unit tests** - MUST be named with the suffix `Test`:
   - ✅ `ScoreCalculatorTest.java`
   - ✅ `SolutionManagerTest.java`
   - ❌ `ScoreCalculatorTests.java` (incorrect - plural)
   - ❌ `TestScoreCalculator.java` (incorrect - prefix)
   - **Executed by**: Maven Surefire plugin during the `test` phase

2. **Integration tests** - MUST be named with the suffix `IT`:
   - ✅ `DatabaseIntegrationIT.java`
   - ✅ `SolverEndToEndIT.java`
   - ❌ `DatabaseIntegrationTest.java` (incorrect - would run as unit test)
   - ❌ `ITDatabaseIntegration.java` (incorrect - prefix)
   - **Executed by**: Maven Failsafe plugin during the `integration-test` phase

**Rationale**: Maven Surefire runs tests matching `**/*Test.java` during the `test` phase, while Maven Failsafe runs tests matching `**/*IT.java` during the `integration-test` phase. This separation ensures unit tests run quickly in every build, while integration tests (which may require external resources or longer execution time) run separately. Consistent naming is critical for proper test execution in CI/CD pipelines.

**Note on performance and stress tests**: While performance regression tests and stress tests are part of feature delivery, they are maintained in other repositories and do not apply to this repository specifically.

**Rationale**: Automated testing catches regressions early, enables confident refactoring, and serves as executable documentation of expected behavior. We mandate test coverage, not development methodology.

### VI. Good Code Hygiene

Code quality is paramount:

- Code MUST be clean, readable, and understandable
- **Readability of code is preferred over ease of writing**
- Follow established coding standards and conventions
- Code is automatically formatted during Maven builds
- CI enforces style conventions

**Code Style and Formatting**:

Code style is based on **standard Java conventions** and is handled automatically during the Maven build:

1. **Automatic formatting** - Run `./mvnw clean install` to format code according to project standards
2. **Newlines** - The formatter intentionally does NOT remove newlines
   - Use newlines **sparingly** to separate logical blocks within methods
   - Excessive blank lines reduce code density and readability
   - Well-placed newlines improve comprehension; poorly-placed ones create clutter
3. **Field ordering consistency** - When multiple class fields are touched in a method, they SHOULD be used/modified in the order they are declared in the class
   - Consistent ordering helps the brain recognize patterns
   - Forgotten field uses stick out when the order is always the same
   - This applies to both reading and writing field values
4. **Prefer imports over fully qualified names** (MUST):
   - Source code MUST use import statements rather than fully qualified class names
   - ✅ Use `import java.util.List;` then `List<String>` in code
   - ❌ Avoid `java.util.List<String>` in code
   - **Exception**: Fully qualified names are REQUIRED only when necessary for compilation (e.g., disambiguating between two classes with the same simple name like `java.util.Date` vs `java.sql.Date`)
   - **Rationale**: Import statements improve code readability by reducing visual clutter and making class names more concise. Fully qualified names should only be used when absolutely necessary to resolve naming conflicts.
5. **Prefer diamond operator** (MUST):
   - Use type inference to eliminate redundant type information
   - **Priority order**: Prefer `var` over diamond operator, as `var` provides better type inference
   - When using `var`, generic types MUST be explicit on the right side (diamond operator cannot be used with `var`)
   - When not using `var`, the diamond operator MUST be used when types can be inferred
   - ✅ **Best**: `var list = new ArrayList<String>();` (var requires explicit type)
   - ✅ **Good**: `List<String> list = new ArrayList<>();` (diamond operator when not using var)
   - ❌ **Incorrect**: `var list = new ArrayList<>();` (compiler error - cannot infer type)
   - ❌ **Incorrect**: `List<String> list = new ArrayList<String>();` (redundant type argument when diamond available)
   - **Rationale**: The `var` keyword provides the best type inference and reduces verbosity. When the left-hand type is explicit, the diamond operator eliminates redundant type information. Both reduce maintenance burden when types change and align with modern Java best practices.
6. Asterisk imports (e.g., `import java.util.*;`) MUST NOT be used.
7. **Manual review** - While formatting is automatic, developers should review for logical clarity

**Code Quality Gates**:

Code quality is monitored by **SonarCloud** in CI with mandatory quality gates:

1. **SonarCloud Quality Grades** (MUST):
   - PRs MUST maintain or improve SonarCloud grades
   - **Reliability**: Grade MUST be B or better (strive for A)
   - **Maintainability**: Grade MUST be B or better (strive for A)
   - PRs that worsen the grade below B will fail CI and cannot be merged

2. **Code Coverage** (MUST):
   - SonarCloud checks code coverage automatically
   - Build WILL FAIL if code coverage falls below the configured threshold (threshold is set in SonarCloud configuration)
   - New code SHOULD maintain or improve overall coverage
   - Focus on meaningful tests, not just coverage numbers

3. **Code Smells and Issues** (SHOULD):
   - Address code smells and issues identified by SonarCloud
   - Critical and major issues SHOULD be resolved before merge
   - Minor issues MAY be addressed or justified

**Rationale**: Code is read far more often than it is written. Readable code reduces maintenance burden, facilitates onboarding, and minimizes bugs. Automatic formatting ensures consistency without bike-shedding. Sparse use of newlines maintains code density while preserving readability. SonarCloud provides objective, automated quality metrics that prevent technical debt accumulation and ensure consistent code quality across the project.

**Visibility and Immutability Principles**:

Code MUST follow defensive design principles to minimize coupling and prevent unintended modifications:

1. **Minimal Visibility by Default** (MUST):
   - Fields and methods MUST be private unless there is a specific reason for broader visibility
   - ✅ `private final ScoreDirector scoreDirector;` (default: private)
   - ❌ `public ScoreDirector scoreDirector;` (incorrect - unnecessarily exposed)
   - Only widen visibility (package-private, protected, public) when required by:
     - Public API contracts (interface implementations, factory methods)
     - Inheritance hierarchies (protected for subclass access)
     - Package-level collaboration (package-private for internal cohesion)
   - **Rationale**: Private-by-default encapsulation prevents accidental coupling, makes refactoring safer, and clearly signals the public contract of a class

2. **Final by Default for Methods and Types** (MUST):
   - Classes and methods MUST be `final` unless there is a specific reason for extension/overriding
   - ✅ `public final class DefaultSolver implements Solver` (cannot be subclassed)
   - ✅ `public final void solve() { ... }` (cannot be overridden in subclasses)
   - ❌ `public class DefaultSolver` (incorrect - allows unintended inheritance)
   - Only omit `final` when:
     - The class/method is explicitly designed for extension (abstract classes, template methods)
     - The type is part of an inheritance hierarchy where subclassing is intended
   - **Note**: When implementing interface methods, marking them `final` prevents further overriding by subclasses but does not affect the interface contract
   - **Rationale**: Final-by-default prevents fragile base class problems, makes behavior predictable, enables compiler optimizations, and documents design intent (this class/method is not meant to be extended/overridden)

3. **Final by Default for Fields** (MUST):
   - Fields MUST be `final` unless there is a specific reason for mutability
   - ✅ `private final List<Move> moveList;` (immutable reference)
   - ✅ `private final int maxValue;` (immutable primitive)
   - ❌ `private List<Move> moveList;` (incorrect - allows reassignment)
   - Only omit `final` when:
     - The field's value must change during the object's lifetime (state machines, caches, lazy initialization)
     - The field is computed/updated after construction (e.g., in lifecycle methods)
   - **Rationale**: Immutable fields prevent accidental modification, make code easier to reason about, enable safe concurrent access, and document that the field's reference (for objects) or value (for primitives) never changes
   - **Note**: `final` for fields means the reference/value is immutable, not the object's internal state. Use immutable collections and value objects where appropriate.

4. **Prefer Immutable Objects** (SHOULD):
   - Favor immutable objects (records, immutable collections) over mutable state
   - ✅ Use `List.of()`, `Set.of()`, `Map.of()` for immutable collections when possible
   - ✅ Use Java records for immutable data carriers: `record Score(int value) { }`
   - Consider using defensive copies when exposing collections from public API
   - **Rationale**: Immutability eliminates entire classes of bugs (race conditions, unexpected mutations), simplifies reasoning about code, and improves thread safety

**Examples**:

```java
// ✅ CORRECT: Minimal visibility, final by default
public final class DefaultSolver<Solution_> implements Solver<Solution_> {
    private final SolverScope<Solution_> solverScope;
    private final Termination<Solution_> termination;
    
    // Package-private constructor (not public - see API Design Principles)
    DefaultSolver(SolverScope<Solution_> solverScope, Termination<Solution_> termination) {
        this.solverScope = Objects.requireNonNull(solverScope);
        this.termination = Objects.requireNonNull(termination);
    }
    
    @Override
    public final Solution_ solve(Solution_ problem) {
        // Implementation
        return solverScope.getBestSolution();
    }
    
    // Private helper - not exposed
    private void initializeSolverScope() {
        // ...
    }
}

// ❌ INCORRECT: Unnecessarily exposed, not final
public class DefaultSolver<Solution_> implements Solver<Solution_> {
    public SolverScope<Solution_> solverScope; // BAD: public field, not final
    protected Termination<Solution_> termination; // BAD: protected without inheritance need
    
    public DefaultSolver(...) { } // BAD: public constructor in API
    
    public Solution_ solve(Solution_ problem) { // BAD: not final, can be overridden
        // ...
    }
}

// ✅ CORRECT: Abstract class intended for extension (omit final appropriately)
public abstract class AbstractPhase<Solution_> implements Phase<Solution_> {
    private final Termination<Solution_> termination; // Still final
    
    protected AbstractPhase(Termination<Solution_> termination) {
        this.termination = Objects.requireNonNull(termination);
    }
    
    // Template method pattern - designed for overriding
    protected abstract void doStep();
    
    // Final method - not meant to be overridden
    public final void step() {
        doStep();
        // Common post-step logic
    }
}
```

**Enforcement**:
- Code reviews MUST check for unnecessary visibility (public/protected when private would suffice)
- Code reviews MUST check for missing `final` modifiers on classes, methods, and fields
- Code reviews SHOULD request justification when `final` is omitted
- Consider adding Checkstyle or ArchUnit rules to enforce these conventions automatically
- SonarCloud may flag some of these as code smells; address them proactively

## Technology Stack and Dependency Constraints

Timefold Solver is a **fast and efficient constraint solver** built with Java. The following technology and dependency rules are MANDATORY to ensure performance, maintainability, and minimal overhead.

### I. Java Language Version

The codebase MUST maintain **JDK 21 compile-time compatibility** while supporting the latest JDK runtime:

1. **Minimum version**: JDK 21 - The codebase must compile and run on JDK 21
2. **Compile-time compatibility**: MUST target JDK 21 - Only features available in JDK 21 are allowed
3. **Runtime compatibility**: SHOULD support the latest available JDK version - The codebase should run on newer JDK releases
4. **Older versions**: JDK 20 and earlier need NOT be supported

**Modern Java features ENCOURAGED**:
- Use all applicable features available in JDK 21 and earlier

**Explicitly AVOIDED features**:
- **Optional** - Avoid using `Optional` in Java. This is an agreed-upon convention to prevent bike-shedding discussions. Use nullable types with proper `@Nullable` annotations from JSpecify instead (see Nullability Policy below).

**Stream API Guidelines**:
- Streams are allowed but should be used judiciously
- Prefer simple for loops when they work and are clearer
- Avoid overuse of streams; they are heavier alternatives to basic iteration
- Use streams when they genuinely improve readability or when working with complex transformations

**Nullability Policy**:

The codebase uses **JSpecify** for nullability annotations with the following conventions:

1. **@NullMarked by default** - Implementations are typically annotated with `@NullMarked`, making everything non-null by default
2. **Null allowed within class implementations** - Internal use of null is permitted for implementation purposes
3. **Null MUST NOT escape through public APIs** - Null values must not be returned from public methods, passed as parameters to external code, or exposed through public fields without explicit `@Nullable` annotation
4. **Use @Nullable explicitly** - When a public API must accept or return null, mark it explicitly with `@Nullable`

**Rationale**: Null is a useful implementation tool but a dangerous public API contract. By keeping null internal to class implementations and using `@NullMarked` with explicit `@Nullable` annotations, we prevent NullPointerExceptions at API boundaries while maintaining implementation flexibility. Once null leaves a class, it can no longer be controlled, making it a potential source of bugs.

**Examples**:
```java
@NullMarked
public class ScoreCalculator {
    // Internal null is OK
    private Solution cachedSolution = null;
    
    // Public API - non-null by default due to @NullMarked
    public Solution calculate(Problem problem) {
        // ...
        return solution; // Must not be null
    }
    
    // Explicit @Nullable when null is part of the contract
    public @Nullable Solution getCachedSolution() {
        return cachedSolution; // May be null
    }
}
```

**Rationale**: JDK 21 is a Long-Term Support (LTS) release with excellent performance, modern language features, and wide industry adoption. 
Maintaining compile-time compatibility with JDK 21 ensures broad compatibility, while supporting the latest JDK runtime ensures users can benefit from the newest JVM improvements. 
Using modern Java features improves code quality, readability, and maintainability, but avoiding `Optional` and overuse of streams prevents unnecessary complexity and performance overhead. 
Controlled use of null within implementations, while preventing it from escaping class boundaries, provides both safety and flexibility.

**Enforcement**: 
- Build configuration MUST target JDK 21 for compilation
- CI MUST verify compilation and tests pass on JDK 21
- CI SHOULD test on all currently supported JDK LTS versions.
- Code reviews MUST verify that null values do not escape class boundaries without explicit `@Nullable` annotations
- Code reviews SHOULD encourage use of modern Java features where appropriate
- Code reviews SHOULD reject use of `Optional`
- Code reviews SHOULD discourage stream overuse
- All packages/classes SHOULD use `@NullMarked` to make non-null the default contract (can be applied at package level in package-info.java or at class level)

### II. Production Code Dependencies

**NO external libraries are allowed in production code** with the following explicit exceptions:

1. **Java Standard Library** - Always allowed
2. **JSpecify annotations** - For nullability annotations (compile-time only, no runtime impact)
3. **Module-specific frameworks** - Required by the nature of specific integration modules:
   - **Quarkus** - Allowed ONLY in Quarkus integration modules
   - **Spring Boot** - Allowed ONLY in Spring integration modules
   - Other integration frameworks ONLY in their respective integration modules
4. **Explicitly constitutional exceptions** - Any other library must be explicitly listed and ratified in this constitution through the amendment procedure

**Rationale**: External dependencies increase attack surface, bloat the binary, introduce version conflicts, and reduce solver performance. The solver core must be lean, fast, and have minimal dependencies to ensure maximum deployment flexibility and performance.

**Enforcement**: Code reviews MUST reject PRs introducing unauthorized production dependencies. Build tools SHOULD enforce dependency constraints where possible.

### III. Test Infrastructure

All tests MUST use the following standardized frameworks:

1. **JUnit** - Required for all test execution
   - Use `@Test`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`, etc.
   - Lifecycle annotations and test orchestration

2. **AssertJ** - Required for ALL assertions
   - MUST use AssertJ fluent assertions: `assertThat(actual).isEqualTo(expected)`
   - FORBIDDEN: JUnit assertions (`assertEquals`, `assertTrue`, etc.)
   - **Rationale**: AssertJ provides superior error messages, fluent API, and better IDE support

3. **Mockito** - Allowed for mocking and stubbing
   - Use for isolating units under test
   - Prefer real objects when practical

4. **Other popular testing frameworks** - Allowed when they provide clear value:
   - Test data builders and fixtures
   - Property-based testing frameworks
   - Performance testing utilities
   - Test containers for integration tests
   - Any framework that improves test quality, readability, or coverage

**Rationale**: Standardized test infrastructure ensures consistent test quality, readable test code, excellent error messages, and reduces the learning curve for contributors. JUnit is the modern standard, and AssertJ provides the best assertion experience in the Java ecosystem.

**Enforcement**: Code reviews MUST reject tests using JUnit assertions instead of AssertJ. CI SHOULD fail on usage of deprecated assertion styles.

### IV. Security

Security is paramount and MUST be considered in all code changes:

**Security Requirements**:

1. **No Secrets in Code** (MUST NOT):
   - Credentials, passwords, API keys, tokens MUST NOT be committed to the repository
   - Use environment variables, configuration files (excluded from version control), or secure secret management
   - Test code MUST NOT contain real credentials; use mock/test credentials only

2. **Dependency Security** (MUST):
   - Dependencies with known high-severity CVEs MUST be addressed promptly
   - Security updates SHOULD be prioritized over feature development
   - **Automated tooling**: Dependabot provides weekly dependency upgrade PRs
   - **Security scanning**: Aikido tooling performs external security checks

3. **Input Validation** (MUST):
   - All external input (user input, file contents, network data) MUST be validated
   - Assume all input is potentially malicious
   - Follow Fail Fast principle (Principle III) for invalid input

4. **Secure Defaults** (MUST):
   - Default configurations MUST be secure
   - Insecure options MUST require explicit opt-in
   - Security-relevant configuration MUST be documented with warnings

5. **Vulnerability Reporting** (MUST):
   - Security vulnerabilities MUST be reported privately to maintainers
   - Do NOT create public issues for security vulnerabilities
   - Follow responsible disclosure practices

6. **Logging Security** (MUST NOT):
   - MUST NOT log sensitive data including credentials, passwords, API keys, tokens, or secrets
   - MUST NOT log personally identifiable information (PII) unless explicitly necessary and compliant with privacy regulations
   - Use appropriate log levels to avoid exposing sensitive information in production logs

**Logging Policy**:

Consistent logging practices improve debuggability while protecting security:

1. **Use SLF4J** - All logging MUST use SLF4J API for framework independence
2. **Log Levels** - The project uses a specific convention for solver lifecycle logging:
   - **ERROR**: Exceptions and unrecoverable failures
   - **WARN**: Recoverable issues, deprecated API usage, configuration problems
   - **INFO**: Solver-level lifecycle events (solver startup, termination, final results)
   - **DEBUG**: Phase-level lifecycle events (phase transitions, phase-level statistics)
   - **TRACE**: Step-level lifecycle events (individual step execution) AND general developer debugging
   - **Note**: This logging convention is under reevaluation but represents current practice
3. **No sensitive data** - MUST NOT log credentials, secrets, PII (see Logging Security above)
4. **Performance awareness** - Minimize logging in hot paths; use conditional logging for expensive message construction; TRACE logging in particular should be careful about performance impact

**Rationale**: Security vulnerabilities can have severe consequences for users. Proactive security practices prevent issues before they occur. The solver's performance-first philosophy must not compromise security - both are essential requirements. Automated tooling (Aikido, Dependabot) provides continuous monitoring, but manual review remains critical. Proper logging practices ensure debuggability without compromising security or performance.

**Enforcement**:
- Code reviews MUST check for hardcoded credentials and secrets
- Aikido performs automated security scanning
- Dependabot automatically proposes dependency updates weekly
- Security-sensitive changes SHOULD receive additional review scrutiny
- Violations of security principles will block PR approval

### Technology Philosophy

- **Performance first** - The solver must be fast; avoid dependencies that compromise performance
- **Minimal footprint** - Keep production dependencies to an absolute minimum
- **Test quality matters** - Invest in excellent testing infrastructure; tests are first-class code
- **Integration flexibility** - Support multiple frameworks through dedicated integration modules, but keep core clean

**Performance Considerations**:

Performance is critical for a constraint solver. While premature optimization should be avoided, performance awareness is expected:

- **Algorithm complexity** - Consider and document time/space complexity for key algorithms; prefer O(n) over O(n²) when practical
- **Hot paths** - Code executed millions of times (inner loops, scoring functions) MUST avoid unnecessary object allocations
- **Benchmarking** - Performance-critical changes SHOULD include before/after benchmarks to verify improvements
- **Documentation** - Document performance characteristics in Javadoc for algorithms where performance matters (e.g., "O(n log n) time complexity")
- **Premature optimization** - Focus first on correctness and clarity; optimize when profiling identifies actual bottlenecks

## Package Structure and API Stability

The codebase is structured into three conceptual parts with different stability guarantees:

### Public API
- Packages containing `api` in their name
- **100% backwards compatible** - Breaking changes ONLY in major versions
- Semantic versioning strictly enforced

### Configuration
- Packages containing `config` in their name
- **100% backwards compatible** - Breaking changes ONLY in major versions
- Configuration changes must maintain migration paths

### Implementation
- All other packages
- **No backwards compatibility guarantees** - May change at any time
- Users depending on implementation classes do so at their own risk

**Versioning Policy**:
- **MAJOR** (e.g., 1.0.0 → 2.0.0): Breaking changes to Public API or Configuration
- **MINOR** (e.g., 1.0.0 → 1.1.0): New features, backwards compatible
- **PATCH** (e.g., 1.0.0 → 1.0.1): Bug fixes, backwards compatible

**Backward Compatibility Testing**:
- Changes to Public API and Configuration packages SHOULD include tests verifying backward compatibility
- Deprecated APIs MUST continue to function correctly until their removal
- Major version upgrades SHOULD include verification that migration paths work as documented

**API Design Principles**:

The public API MUST follow these design principles to ensure stability, flexibility, and maintainability:

1. **Prefer Interfaces over Implementations** (MUST):
   - Public API packages MUST expose interfaces, not concrete implementations
   - ✅ `public interface Solver { ... }` in `api` package
   - ❌ `public class DefaultSolver { ... }` in `api` package (incorrect - exposes implementation)
   - This allows internal implementation changes without breaking the public contract
   - Users program against stable interfaces, not volatile implementations

2. **Hide Implementation Constructors** (MUST):
   - When implementations must be exposed in public API (rare cases only), their constructors MUST be hidden
   - Use factory methods, builders, or dependency injection instead of public constructors
   - ✅ `SolverFactory.create(...)` returns `Solver` interface
   - ✅ Package-private constructor: `DefaultSolver(...) { }` (no visibility modifier)
   - ❌ `public DefaultSolver(...)` in `api` package (incorrect - exposes construction)
   - This controls instantiation and allows internal refactoring

3. **Factory Pattern for Object Creation** (MUST):
   - Object creation in public API MUST use factory methods, factory classes, or builders
   - ✅ `SolverFactory.create(SolverConfig)` → returns `Solver` interface
   - ✅ `SolverManager.create(SolverFactory)` → returns `SolverManager` interface
   - ❌ `new DefaultSolver(config)` (incorrect - direct instantiation of implementation)
   - Factories provide flexibility to change implementations, perform validation, and manage object lifecycle

4. **Return Interfaces from Public Methods** (MUST):
   - Public API methods MUST declare interface return types, not implementation types
   - ✅ `public Solver createSolver()` (returns interface)
   - ❌ `public DefaultSolver createSolver()` (incorrect - returns implementation)
   - Exception: When returning immutable value objects or records with no expected polymorphism

**Rationale**: Exposing interfaces rather than implementations is fundamental to maintainable API design. It provides several critical benefits:

- **Flexibility**: Internal implementations can be changed, optimized, or replaced without breaking user code
- **Stability**: The contract (interface) remains stable even as implementation details evolve
- **Testability**: Users can mock interfaces for testing; mocking concrete classes is harder
- **Evolution**: New implementations can be introduced without API changes (strategy pattern, polymorphism)
- **Encapsulation**: Implementation details remain hidden, reducing coupling and maintenance burden

Hiding constructors and using factories gives the project control over instantiation, enabling validation, dependency injection, caching, and future architectural changes (e.g., introducing proxies, lazy initialization) without breaking existing code.

**Examples**:

```java
// ✅ CORRECT: Public API exposes interface
package ai.timefold.solver.core.api.solver;
public interface Solver<Solution_> {
    Solution_ solve(Solution_ problem);
}

// ✅ CORRECT: Factory returns interface
package ai.timefold.solver.core.api.solver;
public interface SolverFactory<Solution_> {
    static <Solution_> SolverFactory<Solution_> create(SolverConfig solverConfig) {
        // Implementation detail - returns concrete class that implements interface
        return new DefaultSolverFactory<>(solverConfig);
    }
    Solver<Solution_> buildSolver();
}

// ✅ CORRECT: Implementation is package-private or in impl package
package ai.timefold.solver.core.impl.solver;
public class DefaultSolver<Solution_> implements Solver<Solution_> {
    // Package-private constructor (no visibility modifier) - not public
    DefaultSolver(SolverScope<Solution_> solverScope) {
        // ...
    }
    // ... implementation
}

// ❌ INCORRECT: Exposing implementation in API
package ai.timefold.solver.core.api.solver;
public class DefaultSolver<Solution_> implements Solver<Solution_> {
    public DefaultSolver(SolverConfig config) { } // BAD: public constructor
    // ...
}
```

**Enforcement**:
- Code reviews MUST reject PRs that expose implementations in public API packages
- Code reviews MUST reject PRs that add public constructors to implementation classes in API packages
- Architecture tests SHOULD verify that `api` packages only contain interfaces, enums, and immutable value types
- All object creation in public API MUST use factory patterns

## Development Workflow

### Contribution Process

1. **Discuss first** - For significant changes, discuss in GitHub Discussions or Issues before coding
2. **Fork and branch** - Create a feature branch from the main branch
3. **Conventional commits** - Use conventional commit messages (feat, fix, docs, perf, test, build, ci, revert, deps, chore)
4. **Build and test** - Run `./mvnw clean install` to ensure all checks pass
5. **Pull request** - Submit PR; CI will validate compliance
6. **Code review** - Maintainers review for code quality, testing, documentation, and constitution compliance

### Commit Message Format

This project follows the **[Conventional Commits](https://www.conventionalcommits.org/)** specification.

**Required format**: `<type>: <description>`

**Style rules**:
- Use present tense ("Add feature" not "Added feature")
- Use imperative mood ("Move cursor to..." not "Moves cursor to...")
- Reference issues and PRs after the first line

For full details on the specification, see https://www.conventionalcommits.org/

### Build Commands

- 🚀 **Fast build**: `./mvnw clean install -Dquickly` (~1 min) - Skips checks and code analysis
- 🔨 **Normal build**: `./mvnw clean install` (~17 min) - Runs tests and checks, skips documentation
- 📄 **Documentation build**: `./mvnw clean install` in `docs/` (~2 min) - Creates asciidoctor documentation
- 🦾 **Full build**: `./mvnw clean install -Dfull` (~20 min) - All checks, documentation, and distribution files

### Deprecation and Migration Policy

When features evolve or need to be removed, the process MUST be managed carefully to minimize disruption to users:

**Deprecation Requirements**:

1. **Clear marking** - Features targeted for removal or replacement MUST be clearly marked as deprecated:
   - Use `@Deprecated` annotation on classes, methods, and fields
   - Include `@deprecated` Javadoc tag with explanation and alternative
   - Specify the version when deprecated and planned removal version (if known)

2. **Migration path** - Deprecation notices MUST provide:
   - Clear explanation of why the feature is deprecated
   - Recommended alternative or replacement feature
   - Code example showing migration (when practical)
   - Timeline for removal (when known)

3. **OpenRewrite recipes** - When a feature is deprecated, an OpenRewrite migration recipe SHOULD be added to the migration module:
   - Automates the migration from deprecated to new API
   - Reduces manual effort for users
   - Ensures consistent migration patterns
   - If at all possible, provide the recipe; only skip if technically infeasible

**Rationale**: Deprecation is a breaking change for users who must eventually migrate their code. Clear marking and migration support (especially automated via OpenRewrite) significantly reduce the burden on users and demonstrate respect for their investment in the platform. Automated migration recipes transform what would be a painful manual process into a simple command execution.

**Example**:
```java
/**
 * Calculates the score using the old algorithm.
 * 
 * @deprecated Use {@link #calculateScoreOptimized(Solution)} instead.
 *             This method will be removed in version 2.0.0.
 *             The new method provides better performance and accuracy.
 *             
 *             Migration example:
 *             <pre>
 *             // Old code
 *             Score score = calculator.calculateScore(solution);
 *             
 *             // New code
 *             Score score = calculator.calculateScoreOptimized(solution);
 *             </pre>
 */
@Deprecated(since = "1.8.0", forRemoval = true)
public Score calculateScore(Solution solution) {
    // ... implementation
}
```

**OpenRewrite Recipe** (in migration module):
```java
public class MigrateCalculateScore extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate from calculateScore() to calculateScoreOptimized()";
    }
    
    @Override
    public String getDescription() {
        return "Replaces deprecated calculateScore() calls with calculateScoreOptimized().";
    }
    
    // ... recipe implementation
}
```

**Enforcement**:
- Code reviews MUST verify that deprecated features have clear marking and migration guidance
- Code reviews SHOULD verify that OpenRewrite migration recipes exist or are planned
- Deprecated features MUST NOT be removed without at least one major version notice period
- Documentation MUST be updated to reflect deprecation status


## Governance

### Intellectual Property and Licensing

All contributions must respect intellectual property rights and maintain license compatibility:

**License Requirements**:

1. **Apache 2.0 Compatibility** (MUST):
   - All contributions MUST be compatible with the Apache License 2.0
   - Contributors retain their copyright but grant necessary rights for project use
   - By submitting a contribution, contributors agree to license it under Apache 2.0

2. **Third-Party Code** (MUST):
   - Code copied or derived from external sources MUST include proper attribution
   - Original copyright notices MUST be preserved
   - License compatibility MUST be verified before including third-party code
   - Preferred: write original code rather than copying from external sources

3. **License Headers** (NOT REQUIRED):
   - License headers in source files are NOT required
   - The repository-level LICENSE file is sufficient

4. **Contributor Rights** (MUST):
   - Contributors MUST have the right to contribute the code (own it or have permission)
   - Code written as part of employment may require employer approval
   - Contributors affirm they have the necessary rights with each contribution

**Rationale**: Clear intellectual property and licensing practices protect both contributors and users. Apache 2.0 is permissive and widely adopted, making the project accessible while protecting contributor rights. Proper attribution respects original authors and maintains legal compliance.

### Constitution Authority

This constitution supersedes all other development practices and guidelines. All Pull Requests and code reviews MUST verify compliance with constitutional principles.

**Amendment Procedure**:
- Amendments require discussion and approval by maintainers
- Breaking changes to principles require a migration plan for affected code
- Constitution version follows semantic versioning:
  - **MAJOR**: Backward incompatible principle removals or redefinitions
  - **MINOR**: New principles or materially expanded guidance
  - **PATCH**: Clarifications, wording fixes, non-semantic refinements

### Compliance and Enforcement

- All PRs are checked for constitutional compliance during code review
- CI enforces automated checks where possible (commit format, code style, test coverage)
- Violations of principles containing MUST/MUST NOT requirements will block PR approval
- Complexity and deviations from principles must be explicitly justified

### Living Document

This constitution is a living document. As the project evolves, principles may be refined, added, or (rarely) removed through the amendment procedure. The constitution reflects the collective wisdom and values of the Timefold community.

---

**Version**: 2.0.0
