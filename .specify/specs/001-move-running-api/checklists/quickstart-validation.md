# Quickstart Validation Report

**Date**: January 20, 2026  
**Task**: T047 - Validate quickstart.md examples against actual implementation

## Issues Found

### 1. Incorrect Meta Model Access Pattern ❌

**Location**: Multiple examples throughout quickstart.md

**Issue**: The quickstart shows accessing the variable meta model via `view.getVariableMetaModel()`:

```java
// INCORRECT (shown in quickstart)
var variableMetaModel = view.getVariableMetaModel(Task.class, "assignedEmployee");
```

**Problem**: The `MutableSolutionView` interface does NOT have a `getVariableMetaModel()` method.

**Correct Pattern** (from actual implementation in MoveRunnerTest.java):

```java
// CORRECT (actual implementation)
var solutionMetaModel = TaskAssignment.buildSolutionDescriptor().getMetaModel();
var variableMetaModel = solutionMetaModel.entity(Task.class)
        .basicVariable("assignedEmployee", Employee.class);
```

**Impact**: 
- Custom move implementation example (section 3)
- All "Using Builtin Moves" examples at the end of the document
- Users following the quickstart would get compilation errors

### 2. Missing buildSolutionDescriptor() Method Assumption ⚠️

**Issue**: The corrected pattern assumes `TaskAssignment.buildSolutionDescriptor()` exists as a static method.

**Note**: This is likely a test-domain convenience method. Real users would need to use:
```java
SolutionDescriptor.buildSolutionDescriptor(TaskAssignment.class, Task.class).getMetaModel();
```

However, checking the test code, there appears to be a `buildSolutionDescriptor()` convenience method in test domains.

## Recommendations

### Required Fixes

1. **Update Custom Move Implementation** (Section 3):
   - Remove `view.getVariableMetaModel()` call
   - Show meta model obtained from solution descriptor
   - Store meta model in move constructor or obtain it statically

2. **Update All "Using Builtin Moves" Examples**:
   - Example: Testing ChangeMove
   - Example: Testing SwapMove
   - Example: Testing List Variable Moves
   - Example: Testing CompositeMove

3. **Add Meta Model Explanation**:
   - Add a section explaining how to obtain the meta model
   - Show both the direct SolutionDescriptor approach and convenience method (if available)
   - Explain that meta model is typically cached or obtained once

### Suggested Fix Pattern

For custom moves, the meta model should be passed in the constructor:

```java
public class TaskSwapMove implements Move<TaskAssignment> {
    private final PlanningVariableMetaModel<TaskAssignment, Task, Employee> variableMetaModel;
    private final Task leftTask;
    private final Task rightTask;
    
    public TaskSwapMove(
            PlanningVariableMetaModel<TaskAssignment, Task, Employee> variableMetaModel,
            Task leftTask, 
            Task rightTask) {
        this.variableMetaModel = variableMetaModel;
        this.leftTask = leftTask;
        this.rightTask = rightTask;
    }
    
    @Override
    public void execute(MutableSolutionView<TaskAssignment> view) {
        var leftEmployee = view.getValue(variableMetaModel, leftTask);
        var rightEmployee = view.getValue(variableMetaModel, rightTask);
        
        view.changeVariable(variableMetaModel, leftTask, rightEmployee);
        view.changeVariable(variableMetaModel, rightTask, leftEmployee);
    }
}
```

And in tests:

```java
@Test
void testTaskSwapMove() {
    // Get meta model once
    var solutionMetaModel = TaskAssignment.buildSolutionDescriptor().getMetaModel();
    var variableMetaModel = solutionMetaModel.entity(Task.class)
            .basicVariable("assignedEmployee", Employee.class);
    
    // Create test data
    var employee1 = new Employee("Alice");
    var employee2 = new Employee("Bob");
    var task1 = new Task("Task 1");
    task1.setAssignedEmployee(employee1);
    var task2 = new Task("Task 2");
    task2.setAssignedEmployee(employee2);
    
    var solution = new TaskAssignment();
    solution.setTaskList(List.of(task1, task2));
    solution.setEmployeeList(List.of(employee1, employee2));
    
    // Create and execute move
    var move = new TaskSwapMove(variableMetaModel, task1, task2);
    
    try (var runner = MoveRunner.build(TaskAssignment.class, Task.class)) {
        runner.using(solution).execute(move);
    }
    
    assertThat(task1.getAssignedEmployee()).isEqualTo(employee2);
    assertThat(task2.getAssignedEmployee()).isEqualTo(employee1);
}
```

## Status

**Validation Status**: ❌ FAILED - Critical API usage errors found

**Next Steps**:
1. Update quickstart.md with correct meta model access pattern
2. Update all affected code examples
3. Add section explaining meta model acquisition
4. Re-validate all examples compile and run correctly
