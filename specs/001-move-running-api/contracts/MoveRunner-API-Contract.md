# MoveRunner API Contract

**Package**: `ai.timefold.solver.core.preview.api.move`  
**Status**: Preview API (subject to evolution with migration support)  
**Version**: 1.0.0 (initial)  
**Date**: January 19, 2026

## Overview

The MoveRunner API provides a testing utility for executing Move implementations on planning solutions. This contract defines the complete public API surface, including method signatures, exceptions, and behavioral contracts.

---

## Interface Contract

### Factory Method

```java
/**
 * Creates a new MoveRunner for the given solution.
 * <p>
 * The MoveRunner initializes a ScoreDirector and any uninitialized shadow variables
 * in the solution. The MoveRunner instance must be closed after use to prevent resource leaks.
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     runner.execute(myMove);
 *     // Verify solution state with assertions
 * }
 * }</pre>
 *
 * @param solution the planning solution to execute moves on
 * @param <Solution_> the solution type
 * @return a new MoveRunner instance; never null
 */
public static <Solution_> MoveRunner<Solution_> using(Solution_ solution)
```

**Preconditions**:
- `solution` must not be null

**Postconditions**:
- Returns a new MoveRunner instance
- ScoreDirector is created and initialized
- Shadow variables in solution are initialized
- MoveRunner is ready for move execution

**Exceptions**:
- `NullPointerException` if solution is null (via `Objects.requireNonNull`)

---

### Permanent Execution (No Exception Handler)

```java
/**
 * Executes the given move permanently on the solution.
 * <p>
 * The move modifies the solution's planning variables. Any shadow variables are automatically
 * updated. Changes persist after this method returns. The caller is responsible for verifying
 * the solution state with their own assertions.
 * <p>
 * If the move throws an exception, it propagates to the caller and the solution state may be
 * partially modified (no automatic rollback).
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     runner.execute(mySwapMove);
 *     assertThat(entity1.getValue()).isEqualTo(expectedValue);
 * }
 * }</pre>
 *
 * @param move the move to execute
 * @throws IllegalStateException if this MoveRunner has been closed
 * @throws RuntimeException if the move throws an exception during execution
 */
public void execute(Move<Solution_> move)
```

**Preconditions**:
- `move` must not be null
- MoveRunner must not be closed

**Postconditions**:
- Move has been executed via ScoreDirector
- Solution state is modified
- Shadow variables are updated
- Variable listeners have been notified

**Exceptions**:
- `NullPointerException` if move is null (via `Objects.requireNonNull`)
- `IllegalStateException` if MoveRunner has been closed
  - Message format: `"The MoveRunner has been closed and can no longer be used."`
- Any `RuntimeException` thrown by the move propagates to caller

---

### Permanent Execution (With Exception Handler)

```java
/**
 * Executes the given move permanently on the solution with exception handling.
 * <p>
 * This method behaves like {@link #execute(Move)} but provides custom exception handling.
 * If the move throws an exception:
 * <ol>
 *   <li>The exception handler is invoked with the exception</li>
 *   <li>Exception propagation is suppressed (caller continues normally)</li>
 *   <li>Solution state may be partially modified (no automatic rollback)</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     runner.execute(riskyMove, e -> {
 *         logger.error("Move failed", e);
 *         // Handle failure gracefully
 *     });
 *     // Execution continues here even if move threw exception
 * }
 * }</pre>
 *
 * @param move the move to execute
 * @param exceptionHandler callback invoked if move throws exception
 * @throws IllegalStateException if this MoveRunner has been closed
 */
public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler)
```

**Preconditions**:
- `move` must not be null
- `exceptionHandler` must not be null
- MoveRunner must not be closed

**Postconditions**:
- Move has been executed (or attempted)
- If exception occurred:
  - Exception handler was invoked
  - Exception was suppressed (not propagated)
  - Solution state may be partially modified
- If no exception:
  - Same as `execute(move)` variant

**Exceptions**:
- `NullPointerException` if move or exceptionHandler is null (via `Objects.requireNonNull`)
- `IllegalStateException` if MoveRunner has been closed
  - Message format: `"The MoveRunner has been closed and can no longer be used."`

---

### Temporary Execution (With Return Value)

```java
/**
 * Executes the given move temporarily and returns a computed value before undoing.
 * <p>
 * The execution flow:
 * <ol>
 *   <li>Move is executed (solution is modified)</li>
 *   <li>Function is invoked with modified solution, computes and returns a value</li>
 *   <li>Changes are automatically undone (solution restored to original state)</li>
 *   <li>Computed value is returned to caller</li>
 * </ol>
 * <p>
 * This is useful when you need to extract information based on the temporary state,
 * such as reading the score or extracting specific data after the move.
 * <p>
 * In normal flow (no exceptions), the solution is guaranteed to be fully restored to its
 * exact pre-execution state, including shadow variables and all cascading effects.
 * <p>
 * If an exception occurs during move execution, function callback, or undo itself:
 * <ul>
 *   <li>The solution state is UNDEFINED</li>
 *   <li>No restoration is attempted</li>
 *   <li>The solution instance should be discarded</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> Nesting temporary execution calls is not supported and will result
 * in undefined behavior. Do not call executeTemporarily from within the function callback.
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     var score = runner.executeTemporarily(myMove, modifiedSolution -> {
 *         // Read score from solution after move execution
 *         return modifiedSolution.getScore();
 *     });
 *     // Solution is automatically restored here, but we have the score
 *     assertThat(score).isGreaterThan(previousScore);
 * }
 * }</pre>
 *
 * @param move the move to execute temporarily
 * @param function callback invoked with modified solution before undo; computes and returns a value
 * @param <Result_> the type of value returned by the function
 * @return the value computed by the function
 * @throws IllegalStateException if this MoveRunner has been closed
 * @throws RuntimeException if move, function, or undo operations throw an exception
 *         (solution state is UNDEFINED and should be discarded)
 */
public <Result_> Result_ executeTemporarily(Move<Solution_> move, Function<Solution_, Result_> function)
```

**Preconditions**:
- `move` must not be null
- `function` must not be null
- MoveRunner must not be closed
- Not currently in a temporary execution scope (nesting not supported)

**Postconditions (Normal Flow)**:
- Move was executed
- Function callback was invoked
- Solution fully restored to original state
- Shadow variables reverted
- All cascading effects undone
- Computed value returned to caller

**Postconditions (Exception Flow)**:
- Solution state is UNDEFINED
- No restoration attempted
- Solution instance should be discarded
- Exception propagates to caller

**Exceptions**:
- `NullPointerException` if move or function is null (via `Objects.requireNonNull`)
- `IllegalStateException` if MoveRunner has been closed
  - Message format: `"The MoveRunner has been closed and can no longer be used."`
- Any `RuntimeException` from move, function, or undo operations propagates to caller

---

### Temporary Execution (Void - For Assertions)

```java
/**
 * Executes the given move temporarily and automatically undoes the changes.
 * <p>
 * The execution flow:
 * <ol>
 *   <li>Move is executed (solution is modified)</li>
 *   <li>Assertions callback is invoked (user can verify modified state)</li>
 *   <li>Changes are automatically undone (solution restored to original state)</li>
 * </ol>
 * <p>
 * In normal flow (no exceptions), the solution is guaranteed to be fully restored to its
 * exact pre-execution state, including shadow variables and all cascading effects.
 * <p>
 * If an exception occurs during move execution, assertion callback, or undo itself:
 * <ul>
 *   <li>The solution state is UNDEFINED</li>
 *   <li>No restoration is attempted</li>
 *   <li>The solution instance should be discarded</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> Nesting temporary execution calls is not supported and will result
 * in undefined behavior. Do not call executeTemporarily from within the assertions callback.
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     runner.executeTemporarily(myMove, modifiedSolution -> {
 *         // Verify the temporary state
 *         assertThat(entity.getValue()).isEqualTo(expectedValue);
 *     });
 *     // Solution is automatically restored here
 *     assertThat(entity.getValue()).isEqualTo(originalValue);
 * }
 * }</pre>
 *
 * @param move the move to execute temporarily
 * @param assertions callback invoked with modified solution before undo
 * @throws IllegalStateException if this MoveRunner has been closed
 * @throws RuntimeException if move, assertions, or undo operations throw an exception
 *         (solution state is UNDEFINED and should be discarded)
 */
public void executeTemporarily(Move<Solution_> move, Consumer<Solution_> assertions)
```

**Preconditions**:
- `move` must not be null
- `assertions` must not be null
- MoveRunner must not be closed
- Not currently in a temporary execution scope (nesting not supported)

**Postconditions (Normal Flow)**:
- Move was executed
- Assertions callback was invoked
- Solution fully restored to original state
- Shadow variables reverted
- All cascading effects undone

**Postconditions (Exception Flow)**:
- Solution state is UNDEFINED
- No restoration attempted
- Solution instance should be discarded
- Exception propagates to caller

**Exceptions**:
- `NullPointerException` if move or assertions is null (via `Objects.requireNonNull`)
- `IllegalStateException` if MoveRunner has been closed
  - Message format: `"The MoveRunner has been closed and can no longer be used."`
- Any `RuntimeException` from move, assertions, or undo operations propagates to caller

---

### Resource Cleanup

```java
/**
 * Closes this MoveRunner and releases all associated resources.
 * <p>
 * After calling close(), this MoveRunner instance can no longer be used. Any subsequent
 * method calls will throw IllegalStateException.
 * <p>
 * Resources that are released:
 * <ul>
 *   <li>ScoreDirector instance</li>
 *   <li>Solver engine state</li>
 *   <li>Variable listeners</li>
 * </ul>
 * <p>
 * <strong>Important:</strong> Resources will leak if close() is not called. Always use
 * try-with-resources to ensure proper cleanup:
 * <pre>{@code
 * try (var runner = MoveRunner.using(solution)) {
 *     runner.execute(move);
 * } // close() called automatically
 * }</pre>
 *
 * @throws Exception if ScoreDirector cleanup fails (from AutoCloseable)
 */
@Override
public void close() throws Exception
```

**Preconditions**:
- None (can be called multiple times; subsequent calls are no-op)

**Postconditions**:
- ScoreDirector is closed
- All resources released
- MoveRunner is marked as closed
- Subsequent method calls throw IllegalStateException

**Exceptions**:
- May throw exceptions from ScoreDirector cleanup (rare)

---

## Type Parameters

### Solution_

**Constraint**: Must be a planning solution type
- Typically annotated with `@PlanningSolution` (not enforced by MoveRunner)
- Must be mutable (moves modify state)
- Must have entities with planning variables

**Usage**: Captured from factory method `using(solution)` for type safety across all methods

---

## Behavioral Contracts

### Shadow Variable Initialization

**When**: At MoveRunner construction (in `using(solution)` factory method)  
**How**: Delegated to ScoreDirector initialization  
**Guarantee**: Shadow variables are initialized before any move execution

### Undo Mechanism

**Implementation**: Delegated to ScoreDirector's existing undo mechanisms  
**Guarantee (Normal Flow)**: Complete restoration of solution state, including:
- Planning variables restored to original values
- Shadow variables reverted
- Complex object graphs handled correctly
- Cascading effects undone

**Exception Flow**: No undo attempted, state is UNDEFINED

### Exception Handling

| Method | Exception Behavior |
|--------|-------------------|
| `execute(move)` | Exceptions propagate to caller |
| `execute(move, handler)` | Handler invoked, exceptions suppressed |
| `executeTemporarily(move, assertions)` | Exceptions propagate, state UNDEFINED |

### Resource Management

**Pattern**: AutoCloseable with try-with-resources (mandatory)  
**Cleanup**: Delegated to ScoreDirector.close()  
**Leak Consequence**: Score director, solver engine state remain active if not closed

### Thread Safety

**Guarantee**: None - MoveRunner is NOT thread-safe  
**Contract**: Each thread must create its own MoveRunner instance  
**Concurrent Use**: Undefined behavior, not supported

---

## Usage Patterns

### Basic Permanent Execution

```java
try (var runner = MoveRunner.using(solution)) {
    runner.execute(move);
    // Verify with assertions
    assertThat(entity.getValue()).isEqualTo(expected);
}
```

### Permanent Execution with Exception Handling

```java
try (var runner = MoveRunner.using(solution)) {
    runner.execute(riskyMove, e -> logger.error("Failed", e));
    // Continues even if move failed
}
```

### Temporary Execution (Void)

```java
try (var runner = MoveRunner.using(solution)) {
    runner.executeTemporarily(move, modifiedSolution -> {
        // Verify temporary state
        assertThat(entity.getValue()).isEqualTo(tempValue);
    });
    // Solution restored here
    assertThat(entity.getValue()).isEqualTo(originalValue);
}
```

### Temporary Execution (With Return Value)

```java
try (var runner = MoveRunner.using(solution)) {
    var score = runner.executeTemporarily(move, modifiedSolution -> {
        // Read score from solution after move execution
        return modifiedSolution.getScore();
    });
    // Solution restored here, but we have the score
    assertThat(score).isGreaterThan(previousScore);
}
```

### Multiple Executions (Reusable)

```java
try (var runner = MoveRunner.using(solution)) {
    runner.execute(move1);
    runner.execute(move2);
    runner.executeTemporarily(move3, assertions);
    // All valid - runner can be reused
}
```

---

## Anti-Patterns (Do Not Do)

### ❌ Forgetting try-with-resources

```java
var runner = MoveRunner.using(solution);  // Resource leak!
runner.execute(move);
// Missing close() - resources leak
```

### ❌ Nesting temporary execution

```java
runner.executeTemporarily(move1, s1 -> {
    runner.executeTemporarily(move2, s2 -> {  // Undefined behavior!
        // Nesting not supported
    });
});
```

### ❌ Using after close

```java
try (var runner = MoveRunner.using(solution)) {
    runner.execute(move);
} // Closed here
runner.execute(anotherMove);  // IllegalStateException!
```

### ❌ Sharing across threads

```java
var runner = MoveRunner.using(solution);
// Thread 1
thread1.execute(() -> runner.execute(move1));  // Undefined behavior!
// Thread 2
thread2.execute(() -> runner.execute(move2));  // Not thread-safe!
```

---

## Versioning and Stability

**Status**: Preview API  
**Compatibility**: Subject to evolution with migration support  
**Migration Support**: OpenRewrite recipes will be provided when API changes  
**Package**: `preview.api` signals preview status

**Evolution Examples**:
- Method signatures may change in minor versions
- OpenRewrite recipe will automate migration
- Clear deprecation warnings before removal

---

## Summary

This contract defines:
- ✅ Complete method signatures with Javadoc
- ✅ All preconditions and postconditions
- ✅ Exception specifications with message formats
- ✅ Behavioral guarantees (undo, shadow variables, resources)
- ✅ Usage patterns and anti-patterns
- ✅ Thread safety and lifecycle contracts

**Implementation must conform to this contract exactly.**
