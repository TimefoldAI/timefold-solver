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

- Every feature MUST be used in at least one example or quickstart
- Features are only considered complete when they are:
  - Fully tested (unit, integration, and where applicable, performance tests)
  - Fully documented (API docs, user guides, examples)
  - Validated in a realistic scenario

**Documentation Standards**:

Documentation is a first-class requirement for all features:

1. **Public API Documentation** (MUST):
   - All public classes, interfaces, and methods MUST have Javadoc
   - Javadoc MUST include: purpose, parameters (with `@param`), return values (with `@return`), exceptions thrown (with `@throws`)
   - Complex algorithms SHOULD include implementation comments explaining the approach
   - **Note**: `@since` tags are NOT required

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

**Rationale**: Developers spend significant time debugging. Clear error messages reduce support burden and accelerate problem resolution. Proper exception handling ensures errors are visible and debuggable rather than silently lost.

### V. Automated Testing

High-quality automated testing is REQUIRED across all levels:

- **Unit tests** - Test individual components in isolation
- **Integration tests** - Test component interactions and contracts
- **High test coverage** - Aim for comprehensive coverage of critical paths

All code MUST have tests before being merged. The methodology used to develop the code (test-first, test-after, etc.) is left to the discretion of the contributor.

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
4. **Manual review** - While formatting is automatic, developers should review for logical clarity

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

## Technology Stack and Dependency Constraints

Timefold Solver is a **fast and efficient constraint solver** built with Java. The following technology and dependency rules are MANDATORY to ensure performance, maintainability, and minimal overhead.

### I. Java Language Version

The codebase MUST maintain **JDK 17 compile-time compatibility** while supporting the latest JDK runtime:

1. **Minimum version**: JDK 17 - The codebase must compile and run on JDK 17
2. **Compile-time compatibility**: MUST target JDK 17 - Only features available in JDK 17 are allowed
3. **Runtime compatibility**: SHOULD support the latest available JDK version - The codebase should run on newer JDK releases
4. **Older versions**: JDK 16 and earlier need NOT be supported

**Modern Java features ENCOURAGED**:
- Use all applicable features available in JDK 17 and earlier
- **Local variable type inference (var keyword)** (JDK 10+) - Significantly reduces boilerplate by inferring types from initializers. Use when the type is obvious from the right-hand side, improving readability and reducing verbosity. Example: `var list = new ArrayList<String>()` instead of `ArrayList<String> list = new ArrayList<String>()`
- **Records** (JDK 16+) - For immutable data carriers
- **Text blocks** (JDK 15+) - For multi-line strings (e.g., exception messages, SQL, JSON)
- **Pattern matching for instanceof** (JDK 16+) - For cleaner type checks
- **Sealed classes** (JDK 17) - For restricted class hierarchies
- **Switch expressions** (JDK 14+) - For concise, exhaustive switching
- **Stream API** - Use judiciously; prefer simple for loops when they are clearer and more efficient
- **String::formatted** (JDK 13+) - For string formatting (as shown in Principle IV)

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
3. **Null MUST NOT leave the class** - Null values should not escape class boundaries (public APIs, return values, parameters)
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

**Rationale**: JDK 17 is a Long-Term Support (LTS) release with excellent performance, modern language features, and wide industry adoption. Maintaining compile-time compatibility with JDK 17 ensures broad compatibility, while supporting the latest JDK runtime ensures users can benefit from the newest JVM improvements. Using modern Java features improves code quality, readability, and maintainability, but avoiding `Optional` and overuse of streams prevents unnecessary complexity and performance overhead. Controlled use of null within implementations, while preventing it from escaping class boundaries, provides both safety and flexibility.

**Enforcement**: 
- Build configuration MUST target JDK 17 for compilation
- CI MUST verify compilation and tests pass on JDK 17
- CI SHOULD test on all currently supported JDK LTS versions (as of January 2026, that is JDK 17, 21 and 25)
- Code reviews MUST verify that null values do not escape class boundaries without explicit `@Nullable` annotations
- Code reviews SHOULD encourage use of modern Java features where appropriate
- Code reviews SHOULD reject use of `Optional`
- Code reviews SHOULD discourage stream overuse
- All packages/classes SHOULD use `@NullMarked` to make non-null the default contract

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

1. **JUnit Jupiter** (JUnit 6) - Required for all test execution
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

**Rationale**: Standardized test infrastructure ensures consistent test quality, readable test code, excellent error messages, and reduces the learning curve for contributors. JUnit 6 is the modern standard, and AssertJ provides the best assertion experience in the Java ecosystem.

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

**Version**: 1.0.0



