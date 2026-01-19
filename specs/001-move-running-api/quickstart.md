# Quickstart Guide: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026  
**Phase**: Phase 1 - Design & Contracts

## Overview

This quickstart guide demonstrates how to use the Move Running API to test custom move implementations. The API provides a simple, fluent interface for executing moves on planning solutions in both permanent and temporary (with automatic undo) modes.

## Prerequisites

- Java 17 or later
- Timefold Solver 999-SNAPSHOT (or later)
- A planning problem with defined solution and entity classes
- JUnit 5 and AssertJ (for testing)

## Basic Setup

### 1. Import Required Classes

```java
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
```

### 2. Define Your Planning Problem

For this quickstart, we'll use a simple task assignment problem:

```java
@PlanningSolution
public class TaskAssignment {
    @PlanningEntityCollectionProperty
    private List<Task> taskList;
    
    @ValueRangeProvider
    @ProblemFactCollectionProperty
    private List<Employee> employeeList;
    
    // getters, setters, etc.
}

@PlanningEntity
public class Task {
    @PlanningVariable(valueRangeProviderRefs = "employeeRange")
    private Employee assignedEmployee;
    
    // getters, setters, etc.
}
```

### 3. Implement a Custom Move

```java
public class TaskSwapMove implements Move<TaskAssignment> {
    private final Task leftTask;
    private final Task rightTask;
    
    public TaskSwapMove(Task leftTask, Task rightTask) {
        this.leftTask = leftTask;
        this.rightTask = rightTask;
    }
    
    @Override
    public void execute(MutableSolutionView<TaskAssignment> view) {
        var variableMetaModel = view.getVariableMetaModel(Task.class, "assignedEmployee");
        
        var leftEmployee = view.getValue(variableMetaModel, leftTask);
        var rightEmployee = view.getValue(variableMetaModel, rightTask);
        
        view.changeVariable(variableMetaModel, leftTask, rightEmployee);
        view.changeVariable(variableMetaModel, rightTask, leftEmployee);
    }
}
```

## Usage Examples

### Example 1: Permanent Move Execution

Execute a move and verify that the solution was modified:

```java
@Test
void testTaskSwapMove() {
    // Arrange - Create test data
    var employee1 = new Employee("Alice");
    var employee2 = new Employee("Bob");
    
    var task1 = new Task("Task 1");
    task1.setAssignedEmployee(employee1);
    
    var task2 = new Task("Task 2");
    task2.setAssignedEmployee(employee2);
    
    var solution = new TaskAssignment();
    solution.setTaskList(List.of(task1, task2));
    solution.setEmployeeList(List.of(employee1, employee2));
    
    var move = new TaskSwapMove(task1, task2);
    
    // Act - Execute the move
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        context.execute(move);
    }
    
    // Assert - Verify the swap occurred
    assertThat(task1.getAssignedEmployee()).isEqualTo(employee2);
    assertThat(task2.getAssignedEmployee()).isEqualTo(employee1);
}
```

### Example 2: Temporary Move Execution with Automatic Undo

Execute a move temporarily to verify its effects, then automatically restore the original state:

```java
@Test
void testTaskSwapMoveThenUndo() {
    // Arrange
    var employee1 = new Employee("Alice");
    var employee2 = new Employee("Bob");
    
    var task1 = new Task("Task 1");
    task1.setAssignedEmployee(employee1);
    
    var task2 = new Task("Task 2");
    task2.setAssignedEmployee(employee2);
    
    var solution = new TaskAssignment();
    solution.setTaskList(List.of(task1, task2));
    solution.setEmployeeList(List.of(employee1, employee2));
    
    var move = new TaskSwapMove(task1, task2);
    
    // Act & Assert - Execute temporarily
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        
        context.executeTemporarily(move, view -> {
            // Verify changes were applied during temporary scope
            assertThat(task1.getAssignedEmployee()).isEqualTo(employee2);
            assertThat(task2.getAssignedEmployee()).isEqualTo(employee1);
        });
        
        // Verify automatic undo restored original state
        assertThat(task1.getAssignedEmployee()).isEqualTo(employee1);
        assertThat(task2.getAssignedEmployee()).isEqualTo(employee2);
    }
}
```

### Example 3: Exception Handling

Handle exceptions that occur during move execution:

```java
@Test
void testMoveWithExceptionHandler() {
    // Arrange
    var solution = createInvalidSolution(); // Solution that causes move to fail
    var move = new TaskSwapMove(task1, task2);
    var exceptionList = new ArrayList<Exception>();
    
    // Act - Execute with exception handler
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        
        // Exception is caught by handler, execution continues
        context.execute(move, exceptionList::add);
    }
    
    // Assert - Verify exception was handled
    assertThat(exceptionList).hasSize(1);
    assertThat(exceptionList.get(0)).isInstanceOf(IllegalStateException.class);
}
```

### Example 4: Testing Multiple Moves in Sequence

Reuse the MoveRunner to test multiple moves:

```java
@Test
void testMultipleMoves() {
    var solution = createTestSolution();
    var move1 = new TaskSwapMove(task1, task2);
    var move2 = new TaskSwapMove(task2, task3);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        // Execute first move
        var context1 = runner.using(solution);
        context1.execute(move1);
        
        assertThat(task1.getAssignedEmployee()).isEqualTo(employee2);
        
        // Execute second move on the modified solution
        var context2 = runner.using(solution);
        context2.execute(move2);
        
        assertThat(task2.getAssignedEmployee()).isEqualTo(employee3);
    }
}
```

### Example 5: Testing Moves with Shadow Variables

Test moves that affect shadow variables (automatically updated):

```java
@PlanningEntity
public class Task {
    @PlanningVariable(valueRangeProviderRefs = "employeeRange")
    private Employee assignedEmployee;
    
    @ShadowVariable(variableListenerClass = WorkloadUpdater.class, 
                    sourceVariableName = "assignedEmployee")
    private int workload;
    
    // getters, setters, etc.
}

@Test
void testMoveUpdatesShadowVariables() {
    var solution = createTestSolution();
    var move = new TaskSwapMove(task1, task2);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        
        context.executeTemporarily(move, view -> {
            // Shadow variables are automatically updated
            assertThat(task1.getWorkload()).isEqualTo(expectedWorkload1);
            assertThat(task2.getWorkload()).isEqualTo(expectedWorkload2);
        });
        
        // Shadow variables are automatically restored
        assertThat(task1.getWorkload()).isEqualTo(originalWorkload1);
        assertThat(task2.getWorkload()).isEqualTo(originalWorkload2);
    }
}
```

## Common Patterns

### Pattern 1: Parameterized Tests

Test a move with multiple scenarios using `@ParameterizedTest`:

```java
@ParameterizedTest
@MethodSource("provideMoveScenarios")
void testMoveScenarios(Task task1, Task task2, Employee expected1, Employee expected2) {
    var solution = createSolutionWith(task1, task2);
    var move = new TaskSwapMove(task1, task2);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        context.execute(move);
    }
    
    assertThat(task1.getAssignedEmployee()).isEqualTo(expected1);
    assertThat(task2.getAssignedEmployee()).isEqualTo(expected2);
}

static Stream<Arguments> provideMoveScenarios() {
    return Stream.of(
        Arguments.of(task1, task2, employee2, employee1),
        Arguments.of(task3, task4, employee4, employee3)
    );
}
```

### Pattern 2: Testing Move Doability

Verify that a move correctly handles edge cases:

```java
@Test
void testMoveOnSameEmployee() {
    var employee = new Employee("Alice");
    
    var task1 = new Task("Task 1");
    task1.setAssignedEmployee(employee);
    
    var task2 = new Task("Task 2");
    task2.setAssignedEmployee(employee);
    
    var solution = createSolutionWith(task1, task2);
    var move = new TaskSwapMove(task1, task2);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        context.execute(move);
    }
    
    // Verify both tasks still have the same employee (no-op swap)
    assertThat(task1.getAssignedEmployee()).isEqualTo(employee);
    assertThat(task2.getAssignedEmployee()).isEqualTo(employee);
}
```

### Pattern 3: Testing Complex Moves

Test moves that modify multiple variables or entities:

```java
public class ComplexMove implements Move<TaskAssignment> {
    @Override
    public void execute(MutableSolutionView<TaskAssignment> view) {
        // Modify multiple entities
        view.changeVariable(variableMetaModel, task1, newEmployee1);
        view.changeVariable(variableMetaModel, task2, newEmployee2);
        view.changeVariable(variableMetaModel, task3, newEmployee3);
    }
}

@Test
void testComplexMove() {
    var solution = createTestSolution();
    var move = new ComplexMove(/*...*/);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        var context = runner.using(solution);
        
        context.executeTemporarily(move, view -> {
            // Verify all changes were applied
            assertThat(task1.getAssignedEmployee()).isEqualTo(newEmployee1);
            assertThat(task2.getAssignedEmployee()).isEqualTo(newEmployee2);
            assertThat(task3.getAssignedEmployee()).isEqualTo(newEmployee3);
        });
        
        // Verify all changes were undone
        assertThat(task1.getAssignedEmployee()).isEqualTo(originalEmployee1);
        assertThat(task2.getAssignedEmployee()).isEqualTo(originalEmployee2);
        assertThat(task3.getAssignedEmployee()).isEqualTo(originalEmployee3);
    }
}
```

## Best Practices

### ✅ DO

1. **Always use try-with-resources** to ensure proper resource cleanup:
   ```java
   try (var runner = MoveRunner.build(...)) {
       // Use runner here
   }
   ```

2. **Reuse MoveRunner instances** when testing multiple moves:
   ```java
   try (var runner = MoveRunner.build(...)) {
       var context1 = runner.using(solution1);
       context1.execute(move1);
       
       var context2 = runner.using(solution2);
       context2.execute(move2);
   }
   ```

3. **Use AssertJ assertions** for clear error messages:
   ```java
   assertThat(actual).isEqualTo(expected);
   ```

4. **Test both permanent and temporary execution** to verify undo behavior:
   ```java
   context.executeTemporarily(move, view -> {
       // Verify changes during execution
   });
   // Verify undo restored original state
   ```

### ❌ DON'T

1. **Don't share MoveRunner across threads** - it's not thread-safe:
   ```java
   // INCORRECT
   try (var runner = MoveRunner.build(...)) {
       parallelStream.forEach(solution -> 
           runner.using(solution).execute(move) // NOT SAFE
       );
   }
   ```

2. **Don't modify solution state in executeTemporarily callback**:
   ```java
   // INCORRECT
   context.executeTemporarily(move, view -> {
       task.setAssignedEmployee(employee); // Don't do this!
   });
   ```

3. **Don't nest executeTemporarily calls**:
   ```java
   // INCORRECT
   context.executeTemporarily(move1, view1 -> {
       context.executeTemporarily(move2, view2 -> { // Don't do this!
           // ...
       });
   });
   ```

4. **Don't forget to handle exceptions** when using exception handler:
   ```java
   // Make sure your exception handler actually does something useful
   context.execute(move, exception -> {
       logger.error("Move failed", exception); // Good
       // Don't just swallow the exception silently
   });
   ```

## Troubleshooting

### Problem: IllegalStateException - MoveRunner has been closed

**Cause**: Attempting to use MoveRunner after the try-with-resources block has closed it.

**Solution**: Ensure all usage is within the try-with-resources block:
```java
try (var runner = MoveRunner.build(...)) {
    var context = runner.using(solution);
    context.execute(move); // OK - within try block
}
// runner.using(solution); // FAILS - outside try block
```

### Problem: NullPointerException during move execution

**Cause**: Move is null, or solution is null, or required parameters are null.

**Solution**: Validate inputs before creating MoveRunner:
```java
Objects.requireNonNull(solution, "solution must not be null");
Objects.requireNonNull(move, "move must not be null");
```

### Problem: Solution state is corrupted after exception

**Cause**: Exception occurred during `executeTemporarily()`, leaving solution in undefined state.

**Solution**: Discard the solution instance and create a new one:
```java
var solution = createTestSolution();
try {
    try (var runner = MoveRunner.build(...)) {
        runner.using(solution).executeTemporarily(move, assertions);
    }
} catch (Exception e) {
    // Solution is now in undefined state - discard it
    solution = createTestSolution(); // Create fresh instance
}
```

### Problem: Shadow variables not updating correctly

**Cause**: Shadow variables not initialized, or variable listeners not properly configured.

**Solution**: Ensure your shadow variable listener is properly annotated and the MoveRunner build() method will initialize them automatically:
```java
@ShadowVariable(variableListenerClass = WorkloadUpdater.class,
                sourceVariableName = "assignedEmployee")
private int workload;
```

## Next Steps

- Read the [API Contract](../contracts/api-contract.md) for complete API documentation
- Review the [Data Model](../data-model.md) to understand internal structure
- Check the [Research](../research.md) for implementation decisions and rationale
- Explore the solver's move implementations for more complex examples

## Feedback

This API is part of the Preview API and we encourage your feedback:
- [Timefold Solver GitHub Discussions](https://github.com/TimefoldAI/timefold-solver/discussions)
- [Timefold Discord](https://discord.com/channels/1413420192213631086/1414521616955605003)
