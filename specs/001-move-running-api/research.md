# Research: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026  
**Phase**: Phase 0 - Outline & Research

## Overview

This document captures research findings for implementing the Move Running API. The research focuses on understanding the solver's internal mechanisms for move execution, undo operations, and resource management.

**Scope**: The API is designed to support moves on solutions with basic planning variables and list planning variables. Chained planning variables are NOT supported by this API.

## Research Topics

### 1. Solver's Move Execution and Undo Mechanisms

**Question**: How does the solver currently handle move execution and undo operations?

**Findings**:

The solver uses `InnerScoreDirector` to run moves. The score director provides:
- `execute(move)`: Executes a move permanently on the working solution
- `executeTemporarily(move)`: Executes a move and automatically undoes it after execution
- Both methods handle variable listener notifications and shadow variable updates automatically

**Decision**: Use `InnerScoreDirector` methods directly for move execution and undo operations. Introduce a third variant `executeTemporarily(move, consumer)` to allow user code to run between execution and undo.

**Rationale**: The solver already has built-in methods for both permanent and temporary move execution. The `executeTemporarily()` method provides automatic undo functionality that the API needs. By adding a consumer parameter, we can allow users to run assertions after the move executes but before it's undone.

**Alternatives Considered**:
- **Custom undo mechanism**: Rejected - InnerScoreDirector already provides this functionality
- **Solution cloning**: Rejected - memory-intensive and slow compared to using built-in temporary execution
- **MutableSolutionView-based undo**: Rejected - InnerScoreDirector's executeTemporarily() is the correct mechanism

**Implementation Notes**:
- The API will delegate to InnerScoreDirector's execute() and executeTemporarily() methods
- Need to create a variant of executeTemporarily() that accepts a consumer for user assertions
- The score director handles all variable listeners and shadow variable updates automatically

---

### 2. InnerScoreDirector Construction

**Question**: How should the MoveRunner construct and obtain an InnerScoreDirector instance?

**Findings**:

To obtain an `InnerScoreDirector` instance, you need to:
1. Create a `SolutionDescriptor` from the solution and entity classes
2. Use Constraint Streams to create a score director factory with a dummy constraint
3. The dummy constraint is irrelevant (the solver will not run), but must be created using the correct solution types from the solution descriptor
4. The score director factory can then create `InnerScoreDirector` instances

**Decision**: MoveRunner will:
1. Construct a `SolutionDescriptor` from provided solution and entity classes at build() time
2. Create a Constraint Streams configuration with one dummy constraint using the solution types
3. Build a score director factory from this configuration and **store it in MoveRunner** (heavy lifting and caching)
4. MoveRunContext will create fresh `InnerScoreDirector` instances from the cached factory per using() call

**Rationale**: 
- The solver's existing infrastructure requires a score director factory to create score directors
- Constraint Streams with a dummy constraint is the correct way to obtain a score director factory
- The dummy constraint allows us to create a properly configured score director without actually running constraint evaluation
- Validation happens automatically during descriptor construction (fail-fast principle)
- **The score director factory is the heavy object** - creating it involves parsing constraints, building the solution descriptor infrastructure, etc.
- **Score director instances are comparatively lighter** - they can be created quickly from the cached factory
- This design puts heavy lifting (factory creation) in MoveRunner and keeps execution context creation lightweight

**Alternatives Considered**:
- **Direct score director construction**: Not possible - requires a factory
- **Using actual constraints**: Rejected - unnecessary overhead, we don't need score calculation
- **Minimal validation approach**: Rejected - we need the full infrastructure for variable listeners and shadow variable updates
- **Factory creation per execution context**: Rejected - too expensive, factory creation is heavy and should be cached

**Implementation Notes**:
- **MoveRunner stores**: SolutionDescriptor (created once at build() time) and ScoreDirectorFactory (created once at build() time)
- **MoveRunContext stores**: InnerScoreDirector instance (created from cached factory per using() call) and the working solution
- This separation optimizes for reuse: heavy factory creation happens once, lightweight score director creation happens per using()
- All solver exceptions propagate naturally without wrapping (FR-001b)
- Set the working solution on the score director after creation (triggers shadow variable initialization)

---

### 3. Resource Management and Lifecycle

**Question**: What resources need cleanup, and what happens if MoveRunner is not properly closed?

**Findings**:

Based on the feature spec (FR-016, FR-017):
- MoveRunner holds references to score director and solver engine state
- These resources require explicit cleanup to prevent leaks
- The API must implement AutoCloseable and enforce try-with-resources usage

**Decision**: MoveRunner implements AutoCloseable with the following lifecycle:
- **Construction (build())**: Create solution descriptor, validate inputs
- **Execution context (using())**: Create fresh score director for the provided solution
- **Move execution**: Use score director to execute moves with automatic listener/shadow var updates
- **Close**: Release all resources (score director, solver engine state)
- **Post-close**: Throw IllegalStateException on any usage attempts

**Rationale**:
- Prevents resource leaks from unreleased score directors
- Enforces proper resource management through try-with-resources pattern
- Provides clear error messages if misused

**Alternatives Considered**:
- **Automatic cleanup via finalizer**: Unreliable due to GC timing, deprecated in modern Java
- **Weak references**: Complex to implement correctly, doesn't guarantee timely cleanup

**Implementation Notes**:
- MoveRunner tracks closed state with a boolean flag
- All public methods check the closed state and throw IllegalStateException if closed
- Exception message should be clear: "MoveRunner has been closed and cannot be reused"

---

### 4. Exception Handling Strategy

**Question**: How should the API handle exceptions during move execution, especially with the Consumer<Exception> handler?

**Findings**:

From the feature spec:
- FR-001a: Exception handler is `Consumer<Exception>` (handles only Exception subclasses, not Errors)
- FR-013: When handler is provided and invoked, exception propagation is suppressed (caller continues normally)
- Errors propagate naturally and are never suppressed
- No automatic rollback on exception in any case
- For temporary execution, exceptions can be handled within the assertions callback

**Decision**: Implement exception handling as follows:

For permanent execution (`execute(move, exceptionHandler)`):
```java
try {
    // Execute move
} catch (Error e) {
    throw e; // Errors always propagate
} catch (Exception e) {
    if (exceptionHandler != null) {
        exceptionHandler.accept(e);
        // Suppress propagation, caller continues normally
    } else {
        throw e; // No handler, propagate to caller
    }
}
```

For temporary execution (`executeTemporarily(move, assertions)`):
- No exception handler parameter (user handles exceptions within callback)
- If exception occurs during move execution, undo, or callback: solution state is UNDEFINED (FR-007a)
- No attempt to restore state on exception

**Rationale**:
- Clear separation between recoverable exceptions (Exception) and fatal errors (Error)
- Handler invocation suppresses propagation, giving user control over failure handling
- Temporary execution doesn't need handler parameter since callback provides exception handling scope

**Alternatives Considered**:
- **Automatic rollback on exception**: Rejected - user should control failure handling (FR-013)
- **Handler for temporary execution**: Rejected - callback already provides exception handling scope

**Implementation Notes**:
- Use instanceof checks to distinguish Error from Exception
- For temporary execution, document clearly that exceptions leave solution in undefined state
- Thread.currentThread().interrupt() should be called if InterruptedException is caught and suppressed

---

### 5. Thread Safety and Concurrency

**Question**: What thread safety guarantees should the API provide?

**Findings**:

From the spec:
- NFR-001: System MUST NOT be thread-safe (designed for single-threaded test use)
- NFR-002: Each test thread must create its own MoveRunner instance for parallel test execution
- NFR-005: MoveRunner instance reuse is supported (each using() creates fresh context)

**Decision**: 
- No thread safety mechanisms (no synchronization, no volatile fields, no concurrent collections)
- Document clearly that MoveRunner is not thread-safe
- Each thread in parallel test execution must create its own MoveRunner instance

**Rationale**:
- Simplifies implementation (no synchronization overhead)
- Aligns with testing use case (tests typically run in single thread)
- Explicit documentation prevents misuse

**Alternatives Considered**:
- **Thread-safe implementation**: Rejected - unnecessary complexity and performance overhead for testing API
- **Runtime detection of concurrent access**: Rejected - adds overhead, non-functional requirement says no enforcement

**Implementation Notes**:
- Add prominent warning in Javadoc about thread safety
- Example: "This class is NOT thread-safe. Each thread must create its own MoveRunner instance."

---

### 6. Builder Pattern API Design

**Question**: What is the exact API surface for the builder pattern implementation?

**Findings**:

From the spec:
- FR-001c: Builder pattern with method chaining
- FR-001b: Static factory `build(solutionClass, entityClasses...)`
- Entry point returns MoveRunner instance
- MoveRunner exposes `using(solution)` which returns execution context
- Execution context provides `execute(move)`, `execute(move, exceptionHandler)`, and `executeTemporarily(move, function)`

**Decision**: Implement the following API structure:

```java
// Static factory method
MoveRunner<Solution> runner = MoveRunner.build(
    SolutionClass.class, 
    EntityClass1.class, 
    EntityClass2.class
);

// Create execution context (reusable)
try (runner) {
    MoveRunContext<Solution> context = runner.using(solution);
    
    // Permanent execution
    context.execute(move);
    
    // Permanent execution with exception handler
    context.execute(move, exception -> logger.warn("Move failed", exception));
    
    // Temporary execution with assertions
    context.executeTemporarily(move, view -> {
        // Assertions here
        assertThat(view.getValue(...)).isEqualTo(expectedValue);
    });
}
```

**Rationale**:
- Clear separation between runner configuration (build) and solution binding (using)
- Execution context is created fresh per using() call, allowing sequential move execution
- Builder pattern enables fluent API while maintaining type safety

**Alternatives Considered**:
- **Single-use API**: `build().using().execute()` in one chain - rejected because it prevents runner reuse (violates NFR-005)
- **Direct execute on MoveRunner**: `runner.execute(solution, move)` - rejected because it doesn't create reusable execution context

**Implementation Notes**:
- **MoveRunner stores** (heavy lifting and caching):
  - `SolutionDescriptor` - created once at build() time
  - `ScoreDirectorFactory` - created once at build() time from Constraint Streams with dummy constraint
- **MoveRunContext stores** (lightweight per-solution state):
  - `InnerScoreDirector` instance - created from cached factory per using() call
  - Working solution reference - provided by user in using() call
- Both classes validate non-null inputs and check closed state
- This design optimizes for MoveRunner reuse: expensive factory creation once, cheap score director creation per using()

---

### 7. Shadow Variable Initialization

**Question**: How should uninitialized shadow variables in input solutions be handled?

**Findings**:

From the spec:
- FR-012b: System MUST initialize uninitialized shadow variables using solver's underlying mechanisms
- Initialization is handled automatically by the score director
- This happens when the working solution is set on the score director

**Decision**: During MoveRunContext creation (using() method):
1. Create InnerScoreDirector from the score director factory
2. Set the working solution on the score director via `setWorkingSolution(solution)`
3. This automatically triggers shadow variable initialization
4. Shadow variables are now ready for move execution

**Rationale**:
- Fail-fast: Shadow variable initialization happens when solution is bound to the score director
- Automatic: The score director handles this transparently
- Consistency: Uses solver's standard initialization mechanisms
- Per-solution: Each using() call gets a fresh score director with properly initialized shadow variables

**Alternatives Considered**:
- **Manual initialization**: Rejected - the score director does this automatically
- **Build-time initialization**: Rejected - shadow variables are solution-specific, must be initialized per solution

**Implementation Notes**:
- Call `scoreDirector.setWorkingSolution(solution)` in the using() method
- Shadow variable initialization happens automatically as part of this call
- No explicit shadow variable initialization code needed
- If shadow variables can't be initialized, setWorkingSolution() will fail with appropriate exception

---

### 8. Nested Temporary Execution

**Question**: Should the API detect or prevent nested temporary execution calls?

**Findings**:

From the spec:
- NFR-003: Nesting temporary execution API calls is not supported
- No runtime enforcement required
- Document the restriction clearly

**Decision**: 
- No runtime detection or prevention of nested calls
- Document in Javadoc: "Nesting executeTemporarily() calls is not supported and will result in undefined behavior"

**Rationale**:
- Matches spec requirement for no enforcement
- Simpler implementation
- Users responsible for avoiding misuse

**Alternatives Considered**:
- **Runtime detection with ThreadLocal**: Rejected - adds overhead, spec says no enforcement needed
- **Exception on nested call**: Rejected - contradicts "no runtime enforcement" requirement

**Implementation Notes**:
- Add clear warning in executeTemporarily() Javadoc
- No implementation changes needed

---

### 9. Solution State Modification in Temporary Callbacks

**Question**: Should the API detect when user code modifies solution state within executeTemporarily() callback?

**Findings**:

From the spec:
- FR-007b: User code in executeTemporarily callback MUST NOT modify solution state directly
- Violation results in undefined behavior with unpredictable undo results
- NFR-004: Document restriction; no runtime detection required

**Decision**: 
- No runtime detection of solution modifications in callback
- Document clearly in Javadoc with prominent warning
- Example in documentation showing correct usage

**Rationale**:
- Runtime detection would be complex and imperfect
- Spec explicitly says no detection needed
- Documentation and examples sufficient for testing API

**Alternatives Considered**:
- **Defensive copying of solution**: Rejected - expensive and defeats purpose of temporary execution
- **Read-only view in callback**: Rejected - technically complex, prevents legitimate read operations

**Implementation Notes**:
- Javadoc warning: "Do not modify the solution state directly within the callback. Use only the provided view parameter for assertions."
- Code example showing proper usage

---

## Technology Best Practices

### JUnit 5 + AssertJ Testing

**Research**: Best practices for testing the MoveRunner API

**Findings**:
- Constitution mandates JUnit 5 for all tests
- AssertJ required for all assertions (no JUnit assertions)
- Test naming: `*Test.java` for unit tests, `*IT.java` for integration tests

**Best Practices to Apply**:
1. Use `@Test` for test methods
2. Use AssertJ fluent assertions: `assertThat(actual).isEqualTo(expected)`
3. Use `@ParameterizedTest` for testing multiple scenarios
4. Integration tests validate end-to-end execution with real move implementations

---

### Java 17 Modern Features

**Research**: Which Java 17 features should be used in implementation?

**Findings**:
- Constitution encourages modern Java features
- Prefer `var` for local variable type inference
- Use text blocks for multi-line exception messages
- Pattern matching for `instanceof` checks
- Records for immutable data carriers (if applicable)

**Features to Use**:
```java
// var for local variables
var descriptor = SolutionDescriptor.create(...);

// Text blocks for error messages
throw new IllegalStateException("""
    The MoveRunner has been closed and cannot be reused.
    Maybe you forgot to create a new instance within the try-with-resources block?
    """);

// Pattern matching for instanceof (in exception handling)
if (throwable instanceof Error error) {
    throw error;
}
```

---

### @NullMarked and Nullability

**Research**: How to properly use JSpecify nullability annotations?

**Findings**:
- All classes should use `@NullMarked` to make non-null the default
- Use explicit `@Nullable` only when null is part of the contract
- Null MUST NOT leave class boundaries (public APIs)

**Best Practices to Apply**:
```java
@NullMarked
public final class MoveRunner<Solution_> implements AutoCloseable {
    // All parameters non-null by default
    public static <Solution_> MoveRunner<Solution_> build(
            Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        // Validate non-null
        Objects.requireNonNull(solutionClass, "solutionClass");
        Objects.requireNonNull(entityClasses, "entityClasses");
        // ...
    }
    
    // Explicit @Nullable if null is allowed (not applicable for this API)
}
```

---

## Summary

All research topics have been resolved. Key findings:

1. **Move Execution Mechanism**: Use InnerScoreDirector's execute() and executeTemporarily() methods directly
2. **Score Director Construction**: Use Constraint Streams with a dummy constraint to create a score director factory
3. **Resource Management**: Implement AutoCloseable with try-with-resources enforcement
4. **Exception Handling**: Consumer<Exception> suppresses propagation, Errors always propagate
5. **Builder Pattern**: `build(classes) → using(solution) → execute(move)` structure
6. **Shadow Variable Initialization**: Handled automatically by score director when working solution is set
7. **Thread Safety**: Not thread-safe by design, documented clearly
8. **Validation**: Fail-fast at build() time via solution descriptor construction

The design leverages solver internals (InnerScoreDirector, SolutionDescriptor, score director factory) without building custom mechanisms, ensuring consistency with existing solver behavior.
