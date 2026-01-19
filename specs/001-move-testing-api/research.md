# Research: Move Testing API

**Date**: January 19, 2026  
**Feature**: Move Testing API  
**Phase**: 0 - Research & Discovery

## Overview

This document captures the research findings necessary to implement the Move Testing API. The research focuses on understanding the solver's existing move execution and undo mechanisms to leverage them for the testing API.

## Key Research Areas

### 1. Solver's Move Execution Mechanism

**Decision**: Use `MoveDirector` and `EphemeralMoveDirector` classes as the foundation for move testing

**Rationale**:
- The solver already has a sophisticated move execution system through `MoveDirector`
- `MoveDirector` implements `InnerMutableSolutionView<Solution_>` which is the interface that moves expect
- It handles all the complexity of variable changes, shadow variable updates, and listener notifications
- The existing implementation already supports temporary execution with undo via `EphemeralMoveDirector`

**Alternatives considered**:
- Building a custom move execution framework from scratch → Rejected because it would duplicate complex logic for shadow variables, listeners, and state management
- Using `ScoreDirector` directly → Rejected because `MoveDirector` provides a higher-level, move-specific abstraction that's more appropriate

**Key classes**:
- `ai.timefold.solver.core.impl.move.MoveDirector` - Base class for executing moves
- `ai.timefold.solver.core.impl.move.EphemeralMoveDirector` - Supports recording and undoing changes
- `ai.timefold.solver.core.impl.move.RecordedUndoMove` - Represents an undo operation
- `ai.timefold.solver.core.impl.move.VariableChangeRecordingScoreDirector` - Records variable changes for undo

### 2. Undo Mechanism

**Decision**: Leverage `EphemeralMoveDirector` and `VariableChangeRecordingScoreDirector` for temporary execution

**Rationale**:
- The solver already has a battle-tested undo mechanism via `EphemeralMoveDirector`
- It records all variable changes as `ChangeAction` objects during move execution
- When `close()` is called, it automatically undoes all recorded changes
- This mechanism correctly handles shadow variables, listeners, and complex object graphs
- No need to reimplement state capture/restoration logic

**How it works**:
1. Create an `EphemeralMoveDirector` wrapping the `ScoreDirector`
2. Execute the move through the director
3. The director's `VariableChangeRecordingScoreDirector` records all variable changes
4. Call `close()` to undo all changes in reverse order

**Alternatives considered**:
- Deep cloning solutions before/after → Rejected due to performance overhead and complexity
- Manual state tracking → Rejected because shadow variables and listeners make this error-prone
- Using serialization → Rejected due to performance and requirement for serializable solutions

### 3. ScoreDirector Access

**Decision**: Create `ScoreDirectorFactory` automatically within the API using Constraint Streams with a dummy constraint

**Rationale**:
- `MoveDirector` requires an `InnerScoreDirector` to function
- The `ScoreDirector` provides access to the solution model, variable descriptors, and shadow variable infrastructure
- **The move subsystem does not evaluate constraints**, so the actual constraint implementation is irrelevant
- Creating a dummy constraint via Constraint Streams is straightforward and requires no user configuration
- This dramatically simplifies the API - users only need to provide the solution instance
- Eliminates the need for users to manage `SolverFactory` or `ScoreDirectorFactory` in their tests

**Implementation approach**:
```java
// User only provides solution - API creates everything else internally
MoveTester.of(solution);

// Or for testing without full solution
MoveTester.of(solutionClass, entities, facts);
```

**Dummy Constraint Strategy**:
- Use Constraint Streams API to define a trivial constraint (e.g., returns zero score)
- This provides a valid `ScoreDirectorFactory` without requiring actual business constraints
- The factory can create `ScoreDirector` instances for variable tracking and shadow variable management
- Move execution and undo mechanisms work independently of constraint evaluation

**Alternatives considered**:
- Requiring users to provide `ScoreDirectorFactory` → Rejected because it adds unnecessary complexity and boilerplate
- Requiring users to provide their actual constraint configuration → Rejected because constraints aren't used during move testing
- Using `ScoreDirector` directly without a factory → Rejected because we need the factory to create properly configured directors

### 4. Testing Without Full Solution (User Story 3)

**Decision**: Provide a built-in dummy solution class that accepts entities, facts, and optionally a value range

**Rationale**:
- Per spec clarification: "Provide a minimal mock solution object with only the provided entities and facts"
- This allows testing moves in isolation without constructing complete solutions
- Users should not have to provide their own solution class - we provide a generic one
- The dummy solution only needs to satisfy the minimum requirements for the move subsystem to function
- Eliminates the need for users to understand solution class structure for simple move testing

**Implementation approach**:
- Create a built-in `TestingSolution` class with:
  - Generic entity list to hold any planning entities
  - Generic fact list to hold any problem facts
  - Score field of a simple type (e.g., `SimpleScore`)
  - Proper `@PlanningSolution` annotation
- Accept a value range parameter for moves that need value ranges
  - Required for basic variable value ranges
  - Optional for entity-based value ranges (entities themselves can be the value range)
- API automatically creates and populates the testing solution

**API design**:
```java
// With value range (for basic variable moves)
MoveTester.of(entities, facts, valueRange);

// Without value range (for entity-based or list variable moves)
MoveTester.of(entities, facts);
```

**TestingSolution Structure**:
```java
@PlanningSolution
class TestingSolution {
    @PlanningEntityCollectionProperty
    private List<Object> entities;
    
    @ProblemFactCollectionProperty
    private List<Object> facts;
    
    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<Object> valueRange;
    
    @PlanningScore
    private SimpleScore score;
}
```

**Advantages**:
- User provides only data (entities, facts, values) - no class definitions needed
- Works for most move testing scenarios without custom solution classes
- Simplifies API - one less parameter (solution class) to provide
- Automatically compatible with any entity/fact types via generic Object lists

**Challenges**:
- Need to support both value range scenarios (provided vs entity-based)
- Generic Object lists mean no compile-time type safety (acceptable for testing utility)
- Must properly annotate testing solution for solver infrastructure to recognize it

### 5. Exception Handling

**Decision**: Allow users to provide an optional exception handler; propagate exceptions by default

**Rationale**:
- Per spec requirement FR-001a: "System MUST allow developers to optionally provide an exception handler when supplying a Move"
- Per spec requirement FR-013: "System MUST propagate exceptions thrown during move execution to the user-provided exception handler (if provided) or to the caller; no automatic rollback on exception"
- Gives users full control over error handling behavior
- Automatic rollback on exception could hide bugs in move implementations

**API design**:
```java
interface ExceptionHandler<Solution_> {
    void handle(Move<Solution_> move, Exception exception);
}

// Usage
moveTester.onException(handler).execute(move);
```

**Alternatives considered**:
- Always rolling back on exception → Rejected per spec requirements
- Always propagating exceptions → Kept as default, but allow custom handlers
- Logging exceptions → Left to the user's exception handler implementation

### 6. Shadow Variable Handling

**Decision**: Rely on solver's existing shadow variable update mechanisms

**Rationale**:
- Per spec requirement FR-012a: "System MUST NOT suppress or interfere with listeners, shadow variable updates, or other solver subsystems"
- The `MoveDirector` already triggers shadow variable listeners via `triggerVariableListeners()`
- The undo mechanism in `EphemeralMoveDirector` correctly reverts shadow variables
- No special handling needed - just let the system operate normally

**Key insight**:
- Shadow variables are automatically updated when genuine variables change
- The `VariableChangeRecordingScoreDirector` records shadow variable changes too
- Undo operations replay changes in reverse, correctly restoring shadow variable state

### 7. Uninitialized Shadow Variables

**Decision**: Initialize shadow variables before move execution if needed

**Rationale**:
- Per spec requirement FR-012b: "System MUST initialize uninitialized shadow variables in input solutions using the solver's underlying mechanisms before move execution"
- Solutions provided by users might have uninitialized shadow variables
- The `ScoreDirector` has methods to initialize shadow variables

**Implementation**:
```java
// Check if shadow variables need initialization
if (!scoreDirector.isWorkingSolutionInitialized()) {
    scoreDirector.triggerVariableListeners(); // Initializes shadow variables
}
```

**Alternatives considered**:
- Requiring users to initialize shadow variables → Rejected because it adds burden on users
- Failing fast on uninitialized shadow variables → Rejected because we can initialize them automatically

## Technology Decisions

### Testing Framework
- **JUnit 5** - Mandated by constitution for all tests
- **AssertJ** - Mandated by constitution for all assertions
- **Mockito** - May be used for mocking if needed (allowed by constitution)

### Code Organization
- **Public API package**: `ai.timefold.solver.test.api.move`
  - Contains backwards-compatible public API
  - Classes: `MoveTester`, `MoveExecutor`, `TemporaryMoveScope`
- **Implementation package**: `ai.timefold.solver.test.impl.move`
  - Contains internal implementation details
  - May change without notice
  - Classes: `MoveTestSession`, supporting utilities

### Nullability
- Use `@NullMarked` on all API classes per constitution
- Explicitly mark nullable parameters with `@Nullable`
- Keep null confined to implementation internals where possible

### Documentation
- Full Javadoc required on all public API methods
- Include `@param`, `@return`, `@throws` tags
- Code examples in Javadoc where helpful
- No `@since` tags (per constitution)

## Performance Considerations

### Overhead
- Creating an `EphemeralMoveDirector` has minimal overhead
- Recording changes adds small overhead per variable change
- Undo operation replays changes in reverse order (O(n) where n = number of changes)
- Overall performance suitable for testing scenarios (not production solving)

### Memory
- `VariableChangeRecordingScoreDirector` stores a list of `ChangeAction` objects
- Memory proportional to number of variable changes in the move
- Acceptable for testing scenarios with reasonably-sized moves

## Integration Points

### With Solver Core
- Depends on `ai.timefold.solver.core.impl.move.*` classes
- Uses `InnerScoreDirector` and `ScoreDirectorFactory`
- Integrates with existing variable descriptor infrastructure

### With Test Module
- Lives in the `test` module (not `core`)
- Public API exposed for user tests
- Internal implementation uses solver internals

## Open Questions & Risks

### ✅ Resolved
1. How to handle shadow variables? → Use solver's existing mechanisms
2. How to perform undo? → Use `EphemeralMoveDirector`
3. How to test without full solution? → Create minimal mock solution

### 📋 Remaining
None - all major architectural questions have been resolved.

## Next Steps (Phase 1)

1. Design the public API classes in `data-model.md`
2. Define API contracts in `contracts/move-testing-api.yaml`
3. Create usage examples in `quickstart.md`
4. Update agent context with new technology decisions
