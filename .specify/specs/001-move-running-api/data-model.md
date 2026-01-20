# Data Model: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026  
**Phase**: Phase 1 - Design & Contracts

## Overview

This document defines the data model and entities for the Move Running API. Since this is a testing utility API rather than a domain model feature, the "entities" here refer to the API classes and their relationships.

**Scope**: The API supports moves on solutions with basic planning variables and list planning variables only. Chained planning variables are NOT supported.

## Core API Classes

### 1. MoveRunner<Solution_>

**Purpose**: Entry point for the Move Running API. Configures and manages resources for move execution.

**Type**: Final class implementing AutoCloseable

**Lifecycle**: Created via static factory, used within try-with-resources, closed automatically

**Fields**:

| Field Name | Type | Description | Nullability |
|------------|------|-------------|-------------|
| `solutionDescriptor` | `SolutionDescriptor<Solution_>` | Metadata about the solution structure (entities, variables) | Non-null |
| `scoreDirectorFactory` | `ScoreDirectorFactory<Solution_>` | Factory for creating score director instances; built from Constraint Streams with dummy constraint | Non-null |
| `closed` | `boolean` | Tracks whether the runner has been closed | N/A (primitive) |

**Note**: The score director factory is the heavy object created once at build() time. It encapsulates the expensive work of parsing constraints and building solver infrastructure. Score director instances are lightweight and created per using() call from this cached factory.

**Key Methods**:

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `build()` | `Class<Solution_>` solutionClass, `Class<?>...` entityClasses | `MoveRunner<Solution_>` | Static factory method; validates inputs, creates solution descriptor |
| `using()` | `Solution_` solution | `MoveRunContext<Solution_>` | Creates execution context for the given solution; validates non-null |
| `close()` | - | `void` | Releases all resources; sets closed flag to true |

**Validation Rules**:
- `solutionClass` must not be null → `IllegalArgumentException`
- `entityClasses` must not be empty → `IllegalArgumentException`
- Entity classes must be valid planning entities (validated by solver during descriptor construction)
- After close(), any method call → `IllegalStateException`

**State Transitions**:
1. **Created** (via `build()`): Solution descriptor initialized, ready for use
2. **In Use** (via `using()`): Execution contexts created, moves executed
3. **Closed** (via `close()`): Resources released, no further use allowed

**Relationships**:
- Creates: `MoveRunContext<Solution_>` (one per `using()` call)
- Owns: `SolutionDescriptor<Solution_>` (single instance, created at construction)
- Owns: `ScoreDirectorFactory<Solution_>` (single instance, created at construction from Constraint Streams with dummy constraint; used to create score directors for execution contexts)

---

### 2. MoveRunContext<Solution_>

**Purpose**: Binds a solution instance to a MoveRunner and provides methods for executing moves.

**Type**: Final class (holds concrete state: score director reference, solution instance, parent runner reference)

**Lifecycle**: Created per `using()` call, lives until garbage collected (no explicit close needed at this level)

**Fields**:

| Field Name | Type | Description | Nullability |
|------------|------|-------------|-------------|
| `solution` | `Solution_` | The planning solution instance | Non-null |
| `scoreDirector` | `InnerScoreDirector<Solution_, ?>` | Manages score calculation and variable updates | Non-null |
| `moveRunner` | `MoveRunner<Solution_>` | Back-reference to parent runner (for closed check) | Non-null |

**Key Methods**:

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `execute()` | `Move<Solution_>` move | `void` | Executes move permanently; changes persist |
| `execute()` | `Move<Solution_>` move, `Consumer<Exception>` exceptionHandler | `void` | Executes move with exception handling; handler invoked on Exception, propagation suppressed |
| `executeTemporarily()` | `Move<Solution_>` move, `Consumer<SolutionView<Solution_>>` assertions | `void` | Executes move, runs assertions, automatically undoes move |

**Validation Rules**:
- `move` must not be null → `NullPointerException` (via Objects.requireNonNull)
- `solution` must not be null → `IllegalArgumentException` (validated in `using()`)
- Parent MoveRunner must not be closed → `IllegalStateException`

**State Transitions**:
1. **Created** (via `MoveRunner.using()`): Bound to solution, score director initialized
2. **Executing** (during move execution): Move applied, listeners notified, shadow vars updated
3. **Completed** (after execution): Solution in modified state (permanent) or restored state (temporary)

**Relationships**:
- Created by: `MoveRunner<Solution_>`
- Uses: `InnerScoreDirector<Solution_, ?>` for move execution and undo (created from MoveRunner's cached score director factory)
- Accepts: `Move<Solution_>` instances from user

---

### 3. Move<Solution_> (Existing Interface)

**Purpose**: Represents a change operation to be applied to the planning solution.

**Type**: Interface (already exists in `ai.timefold.solver.core.preview.api.move`)

**Key Method**:
- `execute(MutableSolutionView<Solution_>)`: Applies the move to the solution via the mutable view

**Usage in MoveRunner**:
- Provided by user
- Executed via MoveRunContext methods
- `MutableSolutionView` passed to execute() records changes for potential undo

**Validation**: Move implementation is user-provided; MoveRunner validates only that move reference is non-null

---

## Data Flow

### Permanent Move Execution Flow

```
User creates MoveRunner
    ↓
MoveRunner.build(solutionClass, entityClasses...)
    ↓ [validates inputs, creates SolutionDescriptor, creates ScoreDirectorFactory from Constraint Streams with dummy constraint]
MoveRunner instance (with SolutionDescriptor and ScoreDirectorFactory cached)
    ↓
try-with-resources block
    ↓
runner.using(solution)
    ↓ [validates solution, creates InnerScoreDirector from cached factory, sets working solution]
MoveRunContext instance (with InnerScoreDirector)
    ↓
context.execute(move) or context.execute(move, exceptionHandler)
    ↓ [delegates to scoreDirector.execute(move)]
Move modifies solution via MutableSolutionView
    ↓ [listeners notified, shadow vars updated]
Solution in modified state (changes persist)
    ↓
runner.close() (automatic)
    ↓
Resources released (score director factory and any open score directors)
```

### Temporary Move Execution Flow

```
[Same as above through context creation]
    ↓
context.executeTemporarily(move, assertions)
    ↓ [delegates to scoreDirector.executeTemporarily(move) with callback wrapper]
Move modifies solution via MutableSolutionView
    ↓ [listeners notified, shadow vars updated]
Solution in modified state
    ↓
assertions.accept(solutionView) [user assertions run]
    ↓
Automatic undo by InnerScoreDirector (built-in mechanism)
    ↓ [listeners notified again, shadow vars restored]
Solution restored to original state
    ↓
[Continue with normal cleanup]
```

### Exception Flow (Permanent with Handler)

```
context.execute(move, exceptionHandler)
    ↓
Move execution throws Exception
    ↓
catch (Error e) → throw e (Errors always propagate)
    ↓
catch (Exception e) → exceptionHandler.accept(e)
    ↓
Exception suppressed, caller continues normally
```

### Exception Flow (Temporary Execution)

```
context.executeTemporarily(move, assertions)
    ↓
Exception during move, assertions, or undo
    ↓
Solution state is UNDEFINED
    ↓
No undo attempted
    ↓
Exception propagates to caller
    ↓
User must discard solution instance
```

---

## Implementation Package Structure

### Public API (`ai.timefold.solver.core.preview.api.move`)

- `MoveRunner<Solution_>` - Main entry point
- `MoveRunContext<Solution_>` - Execution context interface
- Existing: `Move<Solution_>`, `MutableSolutionView<Solution_>`, `SolutionView<Solution_>`

### Implementation (`ai.timefold.solver.core.impl.move`)

- `DefaultMoveRunner<Solution_>` - Implementation of MoveRunner (if using interface pattern)
- `DefaultMoveRunContext<Solution_>` - Implementation of execution context
- Helper classes (if needed beyond existing InnerScoreDirector)

**Note**: The actual implementation may place MoveRunner and MoveRunContext directly in the public API package as final classes rather than using an interface + implementation split, depending on whether there's value in the abstraction.

---

## Invariants

### Class-Level Invariants

**MoveRunner**:
1. `solutionDescriptor` is never null after construction
2. `scoreDirectorFactory` is never null after construction
3. Once `closed` is true, it remains true (no reopening)
4. All methods (except close) check closed state before proceeding

**MoveRunContext**:
1. `solution` is never null
2. `scoreDirector` is never null
3. Parent `moveRunner` is not closed when context is created

### Method-Level Invariants

**build()**:
- Pre: `solutionClass != null && entityClasses.length > 0`
- Post: Returns non-null MoveRunner with initialized solution descriptor and score director factory (created from Constraint Streams with dummy constraint)

**using()**:
- Pre: `solution != null && !this.closed`
- Post: Returns non-null MoveRunContext with score director created from cached factory and working solution set (shadow variables initialized)

**execute(move)**:
- Pre: `move != null && !parentRunner.closed`
- Post: Solution state modified according to move

**executeTemporarily(move, assertions)**:
- Pre: `move != null && assertions != null && !parentRunner.closed`
- Post (normal flow): Solution state identical to pre-execution state
- Post (exception flow): Solution state is UNDEFINED

---

## Testing Considerations

### Unit Test Data

**Test Entities**:
- Simple planning entity with basic variable (e.g., TestEntity with String value)
- Entity with list variable (e.g., TestEntityWithList)
- Entity with shadow variables

**Test Solutions**:
- Minimal solution with 2-3 entities
- Solution with uninitialized shadow variables (for initialization testing)
- Solution with complex object graph (for undo testing)

**Test Moves**:
- Simple custom move for basic MoveRunner functionality testing (in MoveRunnerTest.java)
- Real solver moves from builtin package for dogfooding:
  - ChangeMove - via `Moves.change()` factory method
  - SwapMove - via `Moves.swap()` factory method
  - ListAssignMove - via `Moves.assign()` factory method
  - ListChangeMove - via `Moves.change()` factory method (list variant)
  - ListSwapMove - via `Moves.swap()` factory method (list variant)
  - CompositeMove - via `Moves.compose()` factory method

### Test Structure

**MoveRunnerTest.java** - Unit tests for MoveRunner API:
1. **Basic Execution**: Execute simple custom move, verify changes applied
2. **Temporary Execution**: Execute move temporarily, verify undo completes
3. **Exception Handling**: Execute move with handler, verify exception suppressed
4. **Resource Management**: Verify close() releases resources, post-close usage throws
5. **Validation**: Verify null checks, empty array checks, closed state checks
6. **Reuse**: Create multiple execution contexts from same runner, verify independence

**builtin/*Test.java** - Dogfooding tests (one per builtin move type):
- **ChangeMoveTest.java**: Test ChangeMove using MoveRunner
- **SwapMoveTest.java**: Test SwapMove using MoveRunner
- **ListAssignMoveTest.java**: Test ListAssignMove using MoveRunner
- **ListChangeMoveTest.java**: Test ListChangeMove using MoveRunner
- **ListSwapMoveTest.java**: Test ListSwapMove using MoveRunner
- **CompositeMoveTest.java**: Test CompositeMove using MoveRunner

Each builtin move test demonstrates real-world usage and validates that MoveRunner works correctly with actual solver moves accessed via the `Moves` factory class.

**No MoveRunnerIT.java needed** - All core functionality can be tested in unit tests without long-running or complex integration testing.

---

## Summary

The data model consists of two primary classes:

1. **MoveRunner**: Manages lifecycle and resources, created once per try-with-resources block. Stores the heavy objects (SolutionDescriptor and ScoreDirectorFactory) created at build() time for reuse.
2. **MoveRunContext**: Binds solution to runner, provides execution methods, created per using() call. Stores lightweight objects (InnerScoreDirector instance created from cached factory, working solution reference).

This separation optimizes for MoveRunner reuse:
- **Heavy lifting happens once** in MoveRunner.build(): Creating SolutionDescriptor and ScoreDirectorFactory from Constraint Streams with dummy constraint
- **Lightweight creation per using()** in MoveRunContext: Creating InnerScoreDirector from cached factory, setting working solution

Both classes coordinate with existing solver infrastructure (SolutionDescriptor, ScoreDirectorFactory, InnerScoreDirector) to provide a simple, fluent API for testing move implementations.

The design emphasizes:
- **Fail-fast validation**: All inputs checked immediately
- **Resource safety**: AutoCloseable enforcement via try-with-resources
- **Simplicity**: Leverages existing solver mechanisms (InnerScoreDirector.execute() and executeTemporarily()) rather than building custom infrastructure
- **Performance**: Caches expensive factory creation in MoveRunner, creates cheap score directors per execution context
- **Flexibility**: Supports both permanent and temporary execution, with or without exception handling
