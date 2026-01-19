# API Contract: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026  
**Phase**: Phase 1 - Design & Contracts

## Overview

This document specifies the public API contract for the Move Running API. The API provides a fluent builder pattern for executing moves on planning solutions in both permanent and temporary (with automatic undo) modes.

## Package Structure

```
ai.timefold.solver.core.preview.api.move
├── MoveRunner<Solution_>           # Main entry point
├── MoveRunContext<Solution_> # Execution context
├── Move<Solution_>                 # Existing interface
├── MutableSolutionView<Solution_>  # Existing interface
└── SolutionView<Solution_>         # Existing interface
```

## API Contracts

### 1. MoveRunner<Solution_>

```java
package ai.timefold.solver.core.preview.api.move;

import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * Entry point for executing {@link Move}s on planning solutions.
 * <p>
 * Provides a fluent API for testing move implementations in both permanent and temporary modes.
 * Designed for testing and development use cases, not production solving workflows.
 * <p>
 * This class is NOT thread-safe. Each thread must create its own MoveRunner instance.
 * <p>
 * Example usage:
 * <pre>{@code
 * try (var runner = MoveRunner.build(SolutionClass.class, EntityClass.class)) {
 *     var context = runner.using(solution);
 *     
 *     // Permanent execution
 *     context.execute(move);
 *     
 *     // Temporary execution with automatic undo
 *     context.executeTemporarily(move, view -> {
 *         assertThat(view.getValue(...)).isEqualTo(expected);
 *     });
 * }
 * }</pre>
 * <p>
 * <strong>This class is part of the Preview API which is under development.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 * Migration support will be provided via OpenRewrite recipes when breaking changes occur.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public final class MoveRunner<Solution_> implements AutoCloseable {

    /**
     * Creates a new MoveRunner for the given solution and entity classes.
     * <p>
     * This method validates inputs, constructs the internal solution descriptor,
     * and creates a score director factory from Constraint Streams with a dummy constraint.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     * <p>
     * This method must be called within a try-with-resources block to ensure proper resource cleanup.
     *
     * @param solutionClass the planning solution class; must not be null
     * @param entityClasses the planning entity classes; must not be empty
     * @param <Solution_> the planning solution type
     * @return a new MoveRunner instance
     * @throws IllegalArgumentException if solutionClass is null or entityClasses is empty
     * @throws Exception if entity classes are not valid planning entities (exceptions from
     *         solver's solution descriptor construction propagate naturally)
     */
    public static <Solution_> MoveRunner<Solution_> build(
            Class<Solution_> solutionClass,
            Class<?>... entityClasses);

    /**
     * Creates an execution context for the given solution instance.
     * <p>
     * This method creates a score director from the cached factory and sets the working solution,
     * which automatically triggers shadow variable initialization for the provided solution.
     * <p>
     * Multiple execution contexts can be created from the same MoveRunner instance,
     * allowing sequential move execution with different solutions or the same solution
     * at different points in time.
     *
     * @param solution the planning solution instance; must not be null
     * @return a new execution context bound to the given solution with initialized shadow variables
     * @throws IllegalArgumentException if solution is null
     * @throws IllegalStateException if this MoveRunner has been closed
     */
    public MoveRunContext<Solution_> using(Solution_ solution);

    /**
     * Releases all resources held by this MoveRunner.
     * <p>
     * After calling this method, any attempt to use the MoveRunner will throw
     * {@link IllegalStateException}.
     * <p>
     * Resources (score director, solver engine state) will leak if this method is not called.
     * Always use try-with-resources to ensure proper cleanup.
     *
     * @throws IllegalStateException if already closed (optional to throw; implementation may ignore)
     */
    @Override
    public void close();
}
```

---

### 2. MoveRunContext<Solution_>

```java
package ai.timefold.solver.core.preview.api.move;

import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for executing moves on a bound planning solution instance.
 * <p>
 * Created via {@link MoveRunner#using(Object)}, this context binds a specific solution
 * instance to the runner and exposes execution methods.
 * <p>
 * This class is NOT thread-safe.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveRunContext<Solution_> {

    /**
     * Executes the given move permanently on the bound solution.
     * <p>
     * Changes made by the move persist after this method returns.
     * Shadow variables are automatically updated via the solver's existing mechanisms.
     * <p>
     * If the move throws an exception and no exception handler is provided,
     * the exception propagates to the caller and the solution state may be partially modified.
     *
     * @param move the move to execute; must not be null
     * @throws NullPointerException if move is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     * @throws Exception if the move throws an exception during execution
     */
    void execute(Move<Solution_> move);

    /**
     * Executes the given move permanently on the bound solution with exception handling.
     * <p>
     * If the move throws an {@link Exception} (not an {@link Error}), the exception handler
     * is invoked and exception propagation is suppressed (caller continues normally).
     * {@link Error}s always propagate and are never suppressed.
     * <p>
     * No automatic rollback occurs on exception. The solution state may be partially modified
     * when an exception occurs. The exception handler allows the caller to control failure handling.
     *
     * @param move the move to execute; must not be null
     * @param exceptionHandler handles exceptions thrown during move execution; must not be null;
     *        invoked only for {@link Exception} subclasses, not {@link Error}s
     * @throws NullPointerException if move or exceptionHandler is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     * @throws Error if the move throws an Error (Errors are never suppressed)
     */
    void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler);

    /**
     * Executes the given move temporarily on the bound solution, runs assertions,
     * then automatically undoes the move.
     * <p>
     * The move is executed, modifying the solution state. The assertions callback is then invoked,
     * allowing the caller to verify the modified state. Finally, the move is automatically undone,
     * restoring the solution to its exact pre-execution state.
     * <p>
     * <strong>Important constraints:</strong>
     * <ul>
     * <li>Nesting executeTemporarily() calls is not supported and results in undefined behavior</li>
     * <li>Do not modify the solution state directly within the assertions callback; doing so
     *     results in undefined behavior with unpredictable undo results</li>
     * <li>If an exception occurs during move execution, assertions callback, or undo operation,
     *     the solution state is UNDEFINED and no restoration is attempted. The caller must
     *     discard the solution instance.</li>
     * </ul>
     *
     * @param move the move to execute temporarily; must not be null
     * @param assertions callback to verify the modified solution state; receives a {@link SolutionView}
     *        for read-only access; must not be null
     * @throws NullPointerException if move or assertions is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     * @throws Exception if the move, assertions, or undo operation throws an exception
     *         (solution state is UNDEFINED when this occurs)
     */
    void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> assertions);
}
```

---

## API Usage Examples

### Example 1: Basic Permanent Execution

```java
@Test
void testSwapMove() {
    var solution = createTestSolution(); // Solution with 2 entities
    var move = new SwapMove(entity1, entity2);
    
    try (var runner = MoveRunner.build(Solution.class, Entity.class)) {
        var context = runner.using(solution);
        context.execute(move);
        
        // Verify changes
        assertThat(entity1.getValue()).isEqualTo("value2");
        assertThat(entity2.getValue()).isEqualTo("value1");
    }
}
```

### Example 2: Permanent Execution with Exception Handler

```java
@Test
void testMoveWithExceptionHandler() {
    var solution = createTestSolution();
    var move = new MoveThrowsException();
    var exceptionsCaught = new ArrayList<Exception>();
    
    try (var runner = MoveRunner.build(Solution.class, Entity.class)) {
        var context = runner.using(solution);
        
        // Exception is suppressed, execution continues
        context.execute(move, exceptionsCaught::add);
        
        assertThat(exceptionsCaught).hasSize(1);
    }
}
```

### Example 3: Temporary Execution with Automatic Undo

```java
@Test
void testTemporaryExecution() {
    var solution = createTestSolution();
    var move = new SwapMove(entity1, entity2);
    var originalValue1 = entity1.getValue();
    var originalValue2 = entity2.getValue();
    
    try (var runner = MoveRunner.build(Solution.class, Entity.class)) {
        var context = runner.using(solution);
        
        context.executeTemporarily(move, view -> {
            // Verify changes were applied
            assertThat(entity1.getValue()).isEqualTo(originalValue2);
            assertThat(entity2.getValue()).isEqualTo(originalValue1);
        });
        
        // Verify undo restored original state
        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
    }
}
```

### Example 4: MoveRunner Reuse

```java
@Test
void testMoveRunnerReuse() {
    try (var runner = MoveRunner.build(Solution.class, Entity.class)) {
        // Execute multiple moves in sequence
        var context1 = runner.using(solution1);
        context1.execute(move1);
        
        var context2 = runner.using(solution2);
        context2.execute(move2);
        
        // Both solutions modified independently
        assertThat(solution1.getEntities()).hasSize(3);
        assertThat(solution2.getEntities()).hasSize(5);
    }
}
```

---

## Error Handling Contract

### Input Validation Errors

| Error Condition | Exception Type | When Thrown |
|----------------|----------------|-------------|
| `solutionClass == null` | `IllegalArgumentException` | `build()` |
| `entityClasses.length == 0` | `IllegalArgumentException` | `build()` |
| Invalid entity classes | Solver exceptions (propagate naturally) | `build()` |
| `solution == null` | `IllegalArgumentException` | `using()` |
| `move == null` | `NullPointerException` | `execute*()` |
| `exceptionHandler == null` | `NullPointerException` | `execute(move, handler)` |
| `assertions == null` | `NullPointerException` | `executeTemporarily()` |

### State Errors

| Error Condition | Exception Type | When Thrown |
|----------------|----------------|-------------|
| MoveRunner already closed | `IllegalStateException` | `using()`, `execute*()` |

### Move Execution Errors

| Error Condition | Behavior |
|----------------|----------|
| Move throws Exception (no handler) | Exception propagates to caller |
| Move throws Exception (with handler) | Handler invoked, propagation suppressed |
| Move throws Error | Error always propagates (never suppressed) |
| Exception during temporary execution | Solution state UNDEFINED, exception propagates |

---

## Backward Compatibility

This API is part of the **Preview** package and is explicitly marked as subject to evolution:

- **Breaking changes are allowed** in minor versions
- **Migration support via OpenRewrite** will be provided when breaking changes occur
- **Deprecation notices** will be provided when APIs are replaced or removed
- **Semantic versioning** applies: major version changes may have breaking changes without OpenRewrite recipes

---

## Thread Safety

**This API is NOT thread-safe.**

Each thread must create its own MoveRunner instance:

```java
// INCORRECT - sharing MoveRunner across threads
try (var runner = MoveRunner.build(...)) {
    parallelStream.forEach(solution -> 
        runner.using(solution).execute(move) // NOT SAFE
    );
}

// CORRECT - each thread creates its own MoveRunner
parallelStream.forEach(solution -> {
    try (var runner = MoveRunner.build(...)) {
        runner.using(solution).execute(move); // SAFE
    }
});
```

---

## Performance Characteristics

- **build()**: O(1) - Creates solution descriptor once
- **using()**: O(1) - Creates score director
- **execute()**: O(changes) - Proportional to number of variable modifications
- **executeTemporarily()**: O(changes) - Execute + undo, both proportional to changes

**Memory overhead**:
- **MoveRunner**: O(1) - Solution descriptor
- **Execution context**: O(1) - Score director and solution reference
- **Temporary execution**: O(changes) - Records undo information

---

## Summary

The Move Running API provides a simple, fluent interface for testing move implementations:

1. **MoveRunner.build()**: Configure runner with solution and entity classes
2. **runner.using()**: Bind solution instance
3. **context.execute()**: Execute move permanently
4. **context.executeTemporarily()**: Execute move with automatic undo

The API enforces proper resource management via AutoCloseable and provides clear error messages for validation failures.
