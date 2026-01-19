# Data Model: Move Testing API

**Date**: January 19, 2026  
**Feature**: Move Testing API  
**Phase**: 1 - Design & Contracts

## Overview

This document defines the class structure and relationships for the Move Testing API. The API provides a fluent interface for executing moves and optionally running them in temporary mode with automatic undo.

## Public API Classes

### 1. MoveTester
**Class Structure**:
```java
@NullMarked
public final class MoveTester<Solution_> implements AutoCloseable {
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    // Private constructor - use factory methods
    private MoveTester(InnerScoreDirector<Solution_, ?> scoreDirector);
    /**
     * Creates a move tester for testing moves on a complete planning solution.
     * Automatically creates a ScoreDirectorFactory using a dummy constraint.
     * 
     * @param solution The planning solution to execute moves on; must not be null
     * @return A new MoveTester instance
     * @throws IllegalArgumentException if solution is null
     */
    public static <Solution_> MoveTester<Solution_> of(Solution_ solution);
    /**
     * Creates a move tester for testing moves on entities without a full solution.
     * Uses a built-in TestingSolution class with the provided entities, facts, and value range.
     * Automatically creates a ScoreDirectorFactory using a dummy constraint.
     * 
     * @param entities The planning entities to include; must not be null
     * @param facts The problem facts to include; may be empty but not null
     * @param valueRange The values to use as default value range; must not be null
     * @return A new MoveTester instance with TestingSolution type
     * @throws IllegalArgumentException if any parameter is null
     */
    public static MoveTester<TestingSolution> of(
            List<?> entities,
            List<?> facts,
            List<?> valueRange);
    
    /**
     * Creates a move tester for testing moves on entities without a full solution.
     * Uses a built-in TestingSolution class with the provided entities and facts.
     * No explicit value range (suitable for entity-based value ranges or list variables).
     * Automatically creates a ScoreDirectorFactory using a dummy constraint.
     * 
     * @param entities The planning entities to include; must not be null
     * @param facts The problem facts to include; may be empty but not null
     * @return A new MoveTester instance with TestingSolution type
     * @throws IllegalArgumentException if any parameter is null
     */
    public static MoveTester<TestingSolution> of(
            List<?> entities,
            List<?> facts);
    /**
     * Executes the move permanently, modifying the solution state.
     * 
     * @param move The move to execute; must not be null
     * @throws IllegalArgumentException if move is null
     */
    public void execute(Move<Solution_> move);
    /**
     * Executes the move temporarily, applying a function while the move is active.
     * The move's changes are visible within the function, then automatically undone.
     * Shadow variables are properly updated during execution and reverted during undo.
     * Exceptions are propagated to the caller.
     * 
     * @param move The move to execute temporarily; must not be null
     * @param function Function to apply while move is applied; must not be null
     * @param <Result_> The type of result returned by the function
     * @return The result produced by the function
     * @throws IllegalArgumentException if move or function is null
     */
    public <Result_> Result_ executeTemporarily(
            Move<Solution_> move,
            Function<Solution_, Result_> function);
    /**
     * Closes the tester and releases resources.
     * Should be called when done testing, typically in a try-with-resources.
     */
    @Override
    public void close();
}
```
**State Management**:
- Owns the `ScoreDirector` lifecycle
- Creates `ScoreDirectorFactory` automatically using Constraint Streams with dummy constraint
- Initializes shadow variables if needed before first move execution
- Thread-safe for single-threaded test scenarios (no concurrent access)
**Validation Rules**:
- All parameters must be non-null (FR-001b)
- Throws `IllegalArgumentException` with descriptive messages for null inputs
**Exception Handling**:
- No special exception handling API - use standard try-catch
- `execute()`: Wrap in try-catch at call site
- `executeTemporarily()`: Handle exceptions within the function or let them propagate
---
## Implementation Classes
### 2. MoveTestSession
**Package**: `ai.timefold.solver.test.impl.move`  
**Purpose**: Internal session managing the lifecycle of move execution.
**Responsibilities**:
- Create and manage `EphemeralMoveDirector` for temporary execution
- Coordinate move execution and undo
- Initialize shadow variables
**Class Structure** (Internal - not public API):
```java
@NullMarked
final class MoveTestSession<Solution_, Score_ extends Score<Score_>> {
    private final InnerScoreDirector<Solution_, Score_> scoreDirector;
    MoveTestSession(InnerScoreDirector<Solution_, Score_> scoreDirector);
    void executePermanently(Move<Solution_> move);
    <Result_> Result_ executeTemporarily(
            Move<Solution_> move,
            Function<Solution_, Result_> function);
    void ensureShadowVariablesInitialized();
}
```
---
### 3. TestingSolution
**Package**: `ai.timefold.solver.test.impl.move`  
**Purpose**: Built-in generic solution class for testing moves without a full solution.
**Responsibilities**:
- Provide a minimal @PlanningSolution implementation
- Hold entities, facts, and value range in generic collections
- Support the move subsystem without requiring user-defined solution classes
**Class Structure** (Internal - not public API):
```java
@PlanningSolution
@NullMarked
final class TestingSolution {
    @PlanningEntityCollectionProperty
    private List<Object> entities;
    @ProblemFactCollectionProperty
    private List<Object> facts;
    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    priv    priv    priv    priv    priv    priv    priv    priv      private SimpleScore score;
    // Constructor and accessor methods
}
```
**Design Notes**:
- Uses generic `Object` lists for maximum flexibility
- No compile-time type safety (acceptable for testing utility)
- Value range is nullable to support entity-based value ranges
- Properly annotated for solver infrastructure recognition
---
### 4. TestingSolutionFactory
**Package**: `ai.timefold.solver.test.impl.move`  
**Purpose**: Creates and populates TestingSolution instances.
**Responsibilities**:
- Instantiate TestingSolution
- Populate entity, fact, and value range collections
- Provide configured solution ready for ScoreDirector
**Class Structure** (Internal - not public API):
```java
@NullMarked
final clafinal clafinal clafinal cl    static TestingSolution createSolution(
            List<?> entities,
            List<?> facts,
            @Nullable List<?> valueRange);
    private TestingSolutionFactory() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```
---
## Class Relationships
```
MoveTester (factory + execution)
    |
    | creates internally
    v
MoveTestSession<Solution_, Score_> (internal)
    |
    | creates for temporary execution
    v
EphemeralMoveDirector (solver internal)
    |
    | wraps
    v
InnerScoreDirector (solver internal)
MoveTester.of(entities, facts, valueRange)
    |
    | uses
    v
TestingSolutionFactory (internal)
    |
    | creates
    v
TestingSolution instance
```
## Entity Validation Rules
Per specification requirements:
1. **Null Validation** (FR-001b):
   - All public API methods validate non-null inputs
   - Throw `IllegalArgumentException` with descriptive messages
   - Include variable name and state in exception messages per constitution
2. **Shadow Variable Initialization** (FR-012b):
   - Check if shadow variables are initialized before move execution
   - Call `scoreDirector.triggerVariableListeners()` if needed
   - Transparent to users - handled automatically
3. **Exception Handling** (FR-013):
   - No automatic rollback on exception
   - Exceptions propagate to caller
   - Use standard try-catch for exception handling
4. **State Restoration** (FR-006, FR-007, FR-010):
   - Leverage `EphemeralMoveDirector.close()` for automatic undo
   - Includes shadow variables and all listener updates
   - Guarantees complete restoration to pre-execution state
## Thread Safety
- **Not thread-safe**: The API is designed for single-threaded test scenarios
- Users should not share `MoveTester` instances across threads
- Each test should create its own `MoveTester`
## Resource Management
- `MoveTester` implements `AutoCloseable` for proper resource cleanup
- Recommended usage in try-with-resources:
  ```java
  try (var tester = MoveTester.of(solution)) {
      tester.execute(move);
      // assertions
  }
  ```
## Error Messages
All error messages follow constitution standards:
- Include variable names and their current values
- Provide actionable advice where applicable
- Use "maybe" prefix for suggestions
Example:
```java
throw new IllegalArgumentException(
    "The move (%s) must not be null.".formatted(move));
throw new IllegalArgumentException(
    """
    The entities list (%s) must no    The ent    Maybe provide an empty list if there are no entities."""
    .formatted(entities));
```
