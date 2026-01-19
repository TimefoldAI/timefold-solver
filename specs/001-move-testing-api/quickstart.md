# Quickstart: Move Testing API

**Date**: January 19, 2026  
**Feature**: Move Testing API  
**Phase**: 1 - Design & Contracts

## Overview

This quickstart demonstrates how to use the Move Testing API to test custom Move implementations. The API supports both permanent and temporary move execution, with proper handling of shadow variables and exception scenarios.

## Prerequisites

- A custom Move implementation
- A planning solution with test data
- JUnit 5 and AssertJ for assertions

## Basic Usage Examples

### Example 1: Testing a Simple Move Permanently

This example shows how to execute a move and verify its effects persist.

```java
import ai.timefold.solver.test.api.move.MoveTester;
import ai.timefold.solver.core.api.solver.SolverFactory;
import static org.assertj.core.api.Assertions.assertThat;

@Test
void testSimpleChangeMove() {
    // Given: A planning solution with test data
    var solution = createTestSolution();
    
    
    // And: A move that changes an entity's variable
    var entity = solution.getEntityList().get(0);
    var newValue = solution.getValueList().get(5);
    var move = new ChangeMove<>(entity, newValue);
    
    // When: Execute the move permanently
    try (var tester = MoveTester.of(solution)) {
        tester.execute(move);
        
        // Then: The change persists
        assertThat(entity.getValue()).isEqualTo(newValue);
    }
}
```

### Example 2: Testing with Temporary Execution

This example shows how to verify a move's effects without modifying the solution permanently.

```java
@Test
void testMoveTemporarily() {
    var solution = createTestSolution();
    
    
    var entity = solution.getEntityList().get(0);
    var originalValue = entity.getValue();
    var newValue = solution.getValueList().get(5);
    var move = new ChangeMove<>(entity, newValue);
    
    try (var tester = MoveTester.of(solution)) {
        // When: Execute temporarily and capture the changed value
        var capturedValue = tester.executeTemporarily(move, s -> {
            // Inside scope: move is applied
            assertThat(entity.getValue()).isEqualTo(newValue);
            return entity.getValue(); // Capture for verification
        });
        
        // Then: After scope, move is undone
        assertThat(entity.getValue()).isEqualTo(originalValue);
        // But we still have the captured value from when move was applied
        assertThat(capturedValue).isEqualTo(newValue);
    }
}
```

### Example 3: Testing with Existing ScoreDirector

If you already have a ScoreDirector (e.g., from a solver), you can use it directly.

```java
@Test
void testMoveWithExistingScoreDirector() {
    
    
    try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
        var solution = createTestSolution();
        scoreDirector.setWorkingSolution(solution);
        
        var move = createMove(solution);
        
        // Use the existing score director
        try (var tester = MoveTester.of(scoreDirector)) {
            tester.execute(move);
            
            // Verify effects
            var updatedSolution = tester.getSolution();
            assertThat(updatedSolution).satisfies(/* assertions */);
        }
    }
}
```

### Example 4: Testing Without a Full Solution

This example shows how to test a move with only specific entities and facts, without building a complete solution. The API provides a built-in dummy solution class.

```java
@Test
void testMoveOnEntitiesOnly() {
    // Given: Just the entities, facts, and values needed for the move
    var entity1 = new Process(1);
    var entity2 = new Process(2);
    var computer1 = new Computer("Server-01");
    var computer2 = new Computer("Server-02");
    
    var entities = List.of(entity1, entity2);
    var facts = List.of(); // No problem facts needed
    var valueRange = List.of(computer1, computer2); // Values for basic variables
    
    // When: Create tester with dummy solution - no solution class needed!
    try (var tester = MoveTester.of(entities, facts, valueRange)) {
        var move = new ChangeMove<>(entity1, computer1);
        tester.execute(move);
        
        // Then: Move effects are visible
        assertThat(entity1.getComputer()).isEqualTo(computer1);
    }
}
```

### Example 4b: Testing Without Value Range (Entity-Based Value Ranges)

This example shows testing when the entities themselves serve as the value range.

```java
@Test
void testMoveWithoutExplicitValueRange() {
    // Given: Entities that serve as their own value range (e.g., chaining)
    var task1 = new Task(1);
    var task2 = new Task(2);
    var task3 = new Task(3);
    
    var entities = List.of(task1, task2, task3);
    var facts = List.of(); // No facts needed
    
    // When: Create tester without value range
    try (var tester = MoveTester.of(entities, facts)) {
        // For list variable moves or entity-based value ranges
        var move = new ListAssignMove<>(task1, task2, 0);
        tester.execute(move);
        
        // Then: Verify the move effect
        assertThat(task2.getTaskList()).contains(task1);
    }
}
```

## Advanced Usage Examples

### Example 5: Handling Exceptions During Move Execution

```java
@Test
void testMoveExceptionHandling() {
    var solution = createTestSolution();
    
    
    var invalidMove = new BrokenMove(); // A move that throws exceptions
    
    try (var tester = MoveTester.of(solution)) {
        // Use standard try-catch for exception handling
        assertThatThrownBy(() -> tester.execute(invalidMove))
            .isInstanceOf(SpecificException.class)
            .hasMessageContaining("expected error message");
            
        // Or handle exceptions directly
        try {
            tester.execute(invalidMove);
            fail("Expected exception");
        } catch (Exception ex) {
            // Custom logging or handling
            System.err.println("Move failed: " + invalidMove.describe());
            assertThat(ex).hasMessageContaining("expected");
        }
    }
}
```

### Example 5b: Handling Exceptions in Temporary Execution

```java
@Test
void testTemporaryExecutionWithExceptionHandling() {
    var solution = createTestSolution();
    
    
    var riskyMove = new RiskyMove();
    
    try (var tester = MoveTester.of(solution)) {
        // Handle exceptions within the function
        var result = tester.executeTemporarily(riskyMove, sol -> {
            try {
                return performRiskyOperation(sol);
            } catch (Exception ex) {
                logger.error("Operation failed", ex);
                return null; // Sentinel value indicating failure
            }
        });
        
        // Check result after undo
        assertThat(result).isNull(); // Operation failed as expected
    }
}
```

### Example 6: Testing Shadow Variable Updates

This example verifies that shadow variables are properly updated after move execution.

```java
@Test
void testShadowVariableUpdates() {
    var solution = createTestSolution();
    
    
    var task = solution.getTaskList().get(0);
    var employee = solution.getEmployeeList().get(0);
    var move = new AssignMove(task, employee);
    
    try (var tester = MoveTester.of(solution)) {
        tester.execute(move);
        
        // Verify genuine variable changed
        assertThat(task.getEmployee()).isEqualTo(employee);
        
        // Verify shadow variable was automatically updated
        assertThat(employee.getTaskList())
            .contains(task);
        assertThat(employee.getTotalLoad())
            .isEqualTo(task.getDuration()); // Shadow variable reflecting the change
    }
}
```

### Example 7: Testing Shadow Variable Restoration

This example verifies that shadow variables are correctly restored during undo.

```java
@Test
void testShadowVariableRestoration() {
    var solution = createTestSolution();
    
    
    var task = solution.getTaskList().get(0);
    var employee1 = solution.getEmployeeList().get(0);
    var employee2 = solution.getEmployeeList().get(1);
    
    // Initially assign to employee1
    task.setEmployee(employee1);
    
    var originalLoad = employee1.getTotalLoad(); // Shadow variable
    var move = new AssignMove(task, employee2);
    
    try (var tester = MoveTester.of(solution)) {
        tester.executeTemporarily(move, s -> {
            // During temporary execution: shadow variable updated
            assertThat(employee1.getTotalLoad())
                .isEqualTo(originalLoad - task.getDuration());
            assertThat(employee2.getTotalLoad())
                .isEqualTo(task.getDuration());
            return null;
        });
        
        // After undo: shadow variables restored
        assertThat(employee1.getTotalLoad()).isEqualTo(originalLoad);
        assertThat(employee2.getTotalLoad()).isZero();
    }
}
```

### Example 8: Testing Complex Moves

This example shows testing a multi-step move with multiple variable changes.

```java
@Test
void testComplexSwapMove() {
    var solution = createTestSolution();
    
    
    var entity1 = solution.getEntityList().get(0);
    var entity2 = solution.getEntityList().get(1);
    var value1 = entity1.getValue();
    var value2 = entity2.getValue();
    
    // A move that swaps values between two entities
    var swapMove = new SwapMove(entity1, entity2);
    
    try (var tester = MoveTester.of(solution)) {
        tester.execute(swapMove);
        
        // Verify the swap occurred
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }
}
```

### Example 9: Verifying Move Behavior in Temporary Mode with Assertions

```java
@Test
void testMoveEffectsDuringTemporaryExecution() {
    var solution = createTestSolution();
    
    
    var entity = solution.getEntityList().get(0);
    var move = createMove(solution);
    
    try (var tester = MoveTester.of(solution)) {
        // Capture state after move execution
        var stateAfterMove = tester.executeTemporarily(move, s -> {
            // Capture the state while move is applied
            return Map.of(
                "entityValue", entity.getValue(),
                "listSize", s.getEntityList().size()
            );
        });
        
        // After undo, verify the captured state
        assertThat(stateAfterMove.get("entityValue")).isNotNull();
        assertThat(stateAfterMove.get("listSize")).isEqualTo(solution.getEntityList().size());
    }
}
```

### Example 10: Testing Move with Multiple Temporary Executions

```java
@Test
void testMultipleTemporaryExecutions() {
    var solution = createTestSolution();
    
    
    var entity = solution.getEntityList().get(0);
    var originalValue = entity.getValue();
    
    try (var tester = MoveTester.of(solution)) {
        // Execute same move multiple times temporarily
        for (int i = 0; i < 5; i++) {
            var newValue = solution.getValueList().get(i);
            var move = new ChangeMove<>(entity, newValue);
            
            tester.executeTemporarily(move, s -> {
                assertThat(entity.getValue()).isEqualTo(newValue);
                return null;
            });
            
            // After each temporary execution, value is restored
            assertThat(entity.getValue()).isEqualTo(originalValue);
        }
    }
}
```

## Common Patterns

### Pattern 1: Arrange-Act-Assert with Permanent Execution

```java
@Test
void standardTestPattern() {
    // Arrange
    var solution = createTestSolution();
    var scoreDirectorFactory = createScoreDirectorFactory();
    var move = createMove(solution);
    
    // Act
    try (var tester = MoveTester.of(solution)) {
        tester.execute(move);
        
        // Assert
        assertThat(solution).satisfies(/* your assertions */);
    }
}
```

### Pattern 2: Capture-Verify with Temporary Execution

```java
@Test
void captureAndVerifyPattern() {
    var solution = createTestSolution();
    var scoreDirectorFactory = createScoreDirectorFactory();
    var move = createMove(solution);
    
    try (var tester = MoveTester.of(solution)) {
        // Capture state during temporary execution
        var capturedState = tester.executeTemporarily(move, s -> {
            return extractRelevantState(s);
        });
        
        // Verify captured state
        assertThat(capturedState).satisfies(/* assertions */);
        
        // Verify restoration
        assertThat(solution).satisfies(/* assertions about restored state */);
    }
}
```

### Pattern 3: Exception Testing

```java
@Test
void exceptionTestingPattern() {
    var solution = createTestSolution();
    var scoreDirectorFactory = createScoreDirectorFactory();
    var brokenMove = new BrokenMove();
    
    try (var tester = MoveTester.of(solution)) {
        // Expect exception without handler
        assertThatThrownBy(() -> tester.execute(brokenMove))
            .isInstanceOf(SpecificException.class)
            .hasMessageContaining("expected message");
    }
}
```

## Best Practices

### 1. Always Use Try-With-Resources

The `MoveExecutor` implements `AutoCloseable`, so always use try-with-resources to ensure proper cleanup:

```java
try (var tester = MoveTester.of(solution)) {
    // Your test code
}
```

### 2. Prefer Temporary Execution for Read-Only Verification

If you only need to verify a move's effects without persisting changes, use temporary execution:

```java
tester.executeTemporarily(move, solution -> {
    // Verify effects
    assertThat(solution.getScore()).isGreaterThan(0);
    return null; // No need to capture state
});
```

### 3. Use Minimal Solutions for Unit Tests

For focused unit tests, create minimal solutions with only the necessary entities and facts:

```java
var executor = MoveTester.of(
    SolutionClass.class,
    List.of(entity1, entity2), // Only entities needed for the move
    List.of(requiredFact),     // Only facts needed for the move
    scoreDirectorFactory
);
```

### 4. Capture Specific State for Assertions

When using temporary execution, capture only the specific state you need to verify:

```java
var changedValue = tester.executeTemporarily(move, s -> 
    s.getEntityList().get(0).getValue()
);
assertThat(changedValue).isEqualTo(expectedValue);
```

### 5. Test Shadow Variables Explicitly

Always verify that shadow variables are properly updated:

```java
tester.execute(move);
assertThat(entity.getGenuineVariable()).isEqualTo(expectedValue);
assertThat(entity.getShadowVariable()).isEqualTo(expectedShadowValue);
```

## Integration with Existing Tests

The Move Testing API integrates seamlessly with existing JUnit 5 tests and AssertJ assertions:

```java
@ExtendWith(SomeExtension.class)
class MyMoveTest {
    
    @BeforeEach
    void setUp() {
        // Standard JUnit setup
    }
    
    @Test
    void testMyCustomMove() {
        // Use MoveTester within standard test structure
        try (var tester = MoveTester.of(solution)) {
            tester.execute(move);
            
            // Use AssertJ assertions as usual
            assertThat(result)
                .isNotNull()
                .extracting(Entity::getValue)
                .containsExactly(expectedValues);
        }
    }
}
```

## Troubleshooting

### Issue: NullPointerException when executing move

**Solution**: Ensure shadow variables are initialized. The API handles this automatically, but if you're seeing this issue, verify that:
1. Your solution descriptor is correctly configured
2. All required @PlanningEntity and @PlanningVariable annotations are present

### Issue: Move effects not visible after execution

**Solution**: Verify you're using permanent execution (`execute()`) rather than temporary execution (`executeTemporarily()`).

### Issue: Shadow variables not updating

**Solution**: Ensure your solution model has proper shadow variable configuration with `@ShadowVariable` annotations and the correct source variable references.

## Next Steps

- Implement your custom Move class following the `Move<Solution_>` interface
- Use the Move Testing API to verify move behavior in unit tests
- Integrate with your existing test suite using JUnit 5 and AssertJ
- Refer to the [API Contract](contracts/move-testing-api.md) for detailed specifications
