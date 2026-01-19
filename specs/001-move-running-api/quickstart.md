# Quickstart: Testing Custom Moves with MoveRunner

**Feature**: Move Running API  
**Target Audience**: Developers implementing custom Move classes  
**Difficulty**: Beginner  
**Time**: 10 minutes

## Overview

Learn how to use the MoveRunner API to test your custom Move implementations. This quickstart covers permanent execution, temporary execution with automatic undo, and exception handling patterns.

---

## Prerequisites

- Java 17 or later
- Timefold Solver dependency in your project
- A planning solution class (annotated with `@PlanningSolution`)
- A custom Move implementation to test

---

## Getting Started

### Step 1: Import the MoveRunner API

```java
import ai.timefold.solver.core.preview.api.move.MoveRunner;
```

**Note**: This is a Preview API located in the `preview.api` package, indicating it may evolve in future versions with migration support.

---

## Basic Usage: Permanent Execution

### Step 2: Execute a Move and Verify Results

The simplest use case is executing a move permanently and verifying the solution state changed as expected.

```java
import static org.assertj.core.api.Assertions.assertThat;

@Test
void testSwapMoveChangesValues() {
    // Given: A solution with two entities
    var solution = createTestSolution();
    var entity1 = solution.getEntityList().get(0);
    var entity2 = solution.getEntityList().get(1);
    var originalValue1 = entity1.getValue();
    var originalValue2 = entity2.getValue();
    
    // And: A move that swaps their values
    var swapMove = new SwapMove(entity1, entity2);
    
    // When: The move is executed
    try (var runner = MoveRunner.using(solution)) {
        runner.execute(swapMove);
    }
    
    // Then: The values are swapped
    assertThat(entity1.getValue()).isEqualTo(originalValue2);
    assertThat(entity2.getValue()).isEqualTo(originalValue1);
}
```

**Key Points**:
- ✅ Always use try-with-resources to prevent resource leaks
- ✅ The solution is permanently modified
- ✅ Shadow variables are automatically updated
- ✅ Use your own test assertions to verify behavior

---

## Temporary Execution: Testing Without Side Effects

### Step 3: Execute a Move Temporarily

When you want to verify a move's behavior without permanently modifying your solution, use `executeTemporarily()`.

```java
@Test
void testMoveProducesExpectedIntermediateState() {
    // Given: A solution
    var solution = createTestSolution();
    var entity = solution.getEntityList().get(0);
    var originalValue = entity.getValue();
    
    // And: A move that changes the entity's value
    var changeMove = new ChangeMove(entity, "newValue");
    
    // When & Then: Execute temporarily
    try (var runner = MoveRunner.using(solution)) {
        runner.executeTemporarily(changeMove, modifiedSolution -> {
            // Inside this callback, the move has been applied
            var modifiedEntity = modifiedSolution.getEntityList().get(0);
            assertThat(modifiedEntity.getValue()).isEqualTo("newValue");
        });
        
        // Outside the callback, changes are automatically undone
        assertThat(entity.getValue()).isEqualTo(originalValue);
    }
}
```

**Key Points**:
- ✅ Solution is automatically restored after the callback completes
- ✅ Perfect for testing "what if" scenarios
- ✅ Shadow variables are also reverted
- ⚠️ Do NOT nest `executeTemporarily()` calls (undefined behavior)

---

### Step 3.5: Execute Temporarily and Return a Value

When you need to extract information based on the temporary state, use the Function variant.

```java
@Test
void testMoveImprovesScore() {
    // Given: A solution
    var solution = createTestSolution();
    
    // And: A move that might improve the score
    var candidateMove = new SwapMove(entity1, entity2);
    
    // When: Execute temporarily and read the score
    try (var runner = MoveRunner.using(solution)) {
        var newScore = runner.executeTemporarily(candidateMove, modifiedSolution -> {
            // Read score from solution after move execution
            // (ScoreDirector handles when it's safe to calculate)
            return modifiedSolution.getScore();
        });
        
        // Then: We can use the score for comparison or analysis
        var currentScore = solution.getScore();
        assertThat(newScore).isGreaterThan(currentScore);
        
        // And: Solution is restored (hasn't changed)
        assertThat(solution.getScore()).isEqualTo(currentScore);
    }
}
```

**Key Points**:
- ✅ Returns the computed value from the callback
- ✅ Solution is automatically restored after computation
- ✅ Read score from solution - don't calculate it yourself
- ✅ Useful for score comparison, feasibility checks, or extracting information
- 💡 Generic return type - can return any type you compute

---

## Exception Handling

### Step 4: Handle Exceptions Gracefully

For moves that might fail, you can provide an exception handler to prevent test failures.

```java
@Test
void testMoveWithExceptionHandler() {
    // Given: A solution and a potentially risky move
    var solution = createTestSolution();
    var riskyMove = new RiskyMove();
    
    // When: Execute with exception handler
    var exceptionOccurred = new AtomicBoolean(false);
    try (var runner = MoveRunner.using(solution)) {
        runner.execute(riskyMove, exception -> {
            // Exception is caught and handled
            exceptionOccurred.set(true);
            System.err.println("Move failed: " + exception.getMessage());
        });
        
        // Then: Execution continues normally (exception suppressed)
        assertThat(exceptionOccurred.get()).isTrue();
    }
}
```

**Key Points**:
- ✅ Exception handler is invoked when move throws an exception
- ✅ Exception propagation is suppressed (test continues)
- ⚠️ Solution state may be partially modified (no automatic rollback)
- 💡 Without a handler, exceptions propagate normally

---

## Advanced Patterns

### Step 5: Multiple Moves in Sequence

You can reuse a MoveRunner instance to execute multiple moves sequentially.

```java
@Test
void testComplexScenarioWithMultipleMoves() {
    var solution = createTestSolution();
    
    try (var runner = MoveRunner.using(solution)) {
        // Execute several moves in sequence
        runner.execute(new Move1());
        runner.execute(new Move2());
        runner.execute(new Move3());
        
        // Verify final state
        assertThat(solution.getScore()).isEqualTo(expectedScore);
    }
}
```

---

### Step 6: Combining Permanent and Temporary Execution

Mix permanent and temporary executions to test complex scenarios.

```java
@Test
void testMixedExecutionScenario() {
    var solution = createTestSolution();
    
    try (var runner = MoveRunner.using(solution)) {
        // Apply move1 permanently
        runner.execute(move1);
        var stateAfterMove1 = captureState(solution);
        
        // Try move2 temporarily (doesn't affect permanent state)
        runner.executeTemporarily(move2, temp -> {
            assertThat(isImprovement(temp)).isTrue();
        });
        
        // Verify move2 was undone
        assertThat(captureState(solution)).isEqualTo(stateAfterMove1);
        
        // Apply move3 permanently
        runner.execute(move3);
    }
}
```

---

## Common Pitfalls

### ❌ Forgetting try-with-resources

```java
// DON'T DO THIS - Resource leak!
var runner = MoveRunner.using(solution);
runner.execute(move);
// Missing close() - resources leak
```

**Solution**: Always use try-with-resources:
```java
// DO THIS
try (var runner = MoveRunner.using(solution)) {
    runner.execute(move);
}
```

---

### ❌ Nesting temporary execution

```java
// DON'T DO THIS - Undefined behavior!
runner.executeTemporarily(move1, s1 -> {
    runner.executeTemporarily(move2, s2 -> {
        // Nesting not supported
    });
});
```

**Solution**: Execute moves sequentially, not nested:
```java
// DO THIS
runner.executeTemporarily(move1, s1 -> {
    assertThat(s1).satisfies(condition1);
});
runner.executeTemporarily(move2, s2 -> {
    assertThat(s2).satisfies(condition2);
});
```

---

### ❌ Using MoveRunner after close

```java
// DON'T DO THIS
var runner = MoveRunner.using(solution);
runner.execute(move1);
runner.close();
runner.execute(move2);  // IllegalStateException!
```

**Solution**: Keep all executions within the try-with-resources block.

---

### ❌ Sharing MoveRunner across threads

```java
// DON'T DO THIS - Not thread-safe!
var runner = MoveRunner.using(solution);
executor.submit(() -> runner.execute(move1));
executor.submit(() -> runner.execute(move2));
```

**Solution**: Each thread creates its own MoveRunner:
```java
// DO THIS
executor.submit(() -> {
    try (var runner = MoveRunner.using(solution1)) {
        runner.execute(move1);
    }
});
executor.submit(() -> {
    try (var runner = MoveRunner.using(solution2)) {
        runner.execute(move2);
    }
});
```

---

## Complete Example: Testing a Custom Swap Move

Here's a complete test class showing all patterns:

```java
package com.example.solver;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CustomSwapMoveTest {
    
    @Test
    void swapMoveShouldExchangeEntityValues() {
        // Given
        var solution = TestDataBuilder.createSolution();
        var entity1 = solution.getEntities().get(0);
        var entity2 = solution.getEntities().get(1);
        var value1 = entity1.getValue();
        var value2 = entity2.getValue();
        
        // When
        try (var runner = MoveRunner.using(solution)) {
            runner.execute(new SwapMove(entity1, entity2));
        }
        
        // Then
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }
    
    @Test
    void swapMoveTemporarilyShouldRestoreSolution() {
        // Given
        var solution = TestDataBuilder.createSolution();
        var entity = solution.getEntities().get(0);
        var originalValue = entity.getValue();
        
        // When
        try (var runner = MoveRunner.using(solution)) {
            runner.executeTemporarily(
                new SwapMove(entity, solution.getEntities().get(1)),
                temp -> {
                    // Verify swap occurred
                    assertThat(entity.getValue()).isNotEqualTo(originalValue);
                }
            );
        }
        
        // Then - restored
        assertThat(entity.getValue()).isEqualTo(originalValue);
    }
    
    @Test
    void invalidMoveShouldBeHandledGracefully() {
        // Given
        var solution = TestDataBuilder.createSolution();
        var invalidMove = new InvalidMove();
        
        // When
        var exceptionCaught = new AtomicBoolean(false);
        try (var runner = MoveRunner.using(solution)) {
            runner.execute(invalidMove, e -> {
                exceptionCaught.set(true);
                assertThat(e).hasMessageContaining("Invalid move");
            });
        }
        
        // Then
        assertThat(exceptionCaught).isTrue();
    }
}
```

---

## Best Practices

1. **Always use try-with-resources** - Prevents resource leaks
2. **Use AssertJ for assertions** - Required by Timefold conventions
3. **Test both success and failure paths** - Include exception scenarios
4. **One MoveRunner per test** - Don't share instances across tests
5. **Document your test intent** - Clear Given/When/Then structure
6. **Test shadow variable updates** - Verify cascading effects
7. **Keep temporary execution simple** - Don't nest, don't reuse solution state

---

## Next Steps

- **Implement your Move class** - Follow Move interface contract
- **Add comprehensive tests** - Cover edge cases and exceptions
- **Use in integration tests** - Test with real solver configurations
- **Check solver documentation** - Learn about other testing utilities

---

## Reference

- **API Package**: `ai.timefold.solver.core.preview.api.move`
- **Main Class**: `MoveRunner`
- **Status**: Preview API (subject to evolution)
- **Migration Support**: OpenRewrite recipes provided when API changes

---

## Questions or Issues?

- Check the Timefold Solver user guide
- Browse the API Javadoc
- Ask on Timefold community forums
- Report bugs via GitHub issues

---

**Happy testing! 🎯**
