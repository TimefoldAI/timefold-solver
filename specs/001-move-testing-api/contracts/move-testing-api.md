# Move Testing API Contract

**Version**: 1.0.0  
**Date**: January 19, 2026  
**Package**: `ai.timefold.solver.test.api.move`

## Overview

This contract defines the public API for testing Move implementations in Timefold Solver. The API is designed for use in unit and integration tests to verify that custom Move implementations behave correctly.

## Public API Surface

### 1. MoveTester (Main API Class)

**Fully Qualified Name**: `ai.timefold.solver.test.api.move.MoveTester`

**Purpose**: Executes moves in permanent or temporary mode for testing

**Implements**: `AutoCloseable`

**Factory Methods**:

#### of(Solution)
```
Signature: public static <Solution_> MoveTester<Solution_> of(
              Solution_ solution)
              
Parameters:
  - solution: The planning solution to execute moves on (must not be null)
  
Returns: A new MoveTester instance configured with the given solution

Throws:
  - IllegalArgumentException if solution is null
  
Preconditions:
  - Solution must be a valid planning solution with proper annotations
  
Postconditions:
  - Returns a usable MoveTester
  - ScoreDirectorFactory is created automatically with dummy constraint
  - Shadow variables are NOT initialized until first move execution
  
Implementation Notes:
  - API creates ScoreDirectorFactory internally using Constraint Streams
  - Uses a dummy constraint (constraints are not evaluated during move testing)
```

#### of(List, List, List)
```
Signature: public static MoveTester<TestingSolution> of(
              List<?> entities,
              List<?> facts,
              List<?> valueRange)
              
Parameters:
  - entities: Planning entities to include (must not be null, may be empty)
  - facts: Problem facts to include (must not be null, may be empty)
  - valueRange: Values to use as default value range (must not be null, may be empty)
  
Returns: A new MoveTester instance with TestingSolution type

Throws:
  - IllegalArgumentException if any parameter is null
  
Preconditions:
  - All parameters must be non-null lists (can be empty)
  
Postconditions:
  - Creates a TestingSolution instance with provided entities, facts, and value range
  - ScoreDirectorFactory is created automatically with dummy constraint
  - Returns a usable MoveTester<TestingSolution>
  
Implementation Notes:
  - Uses built-in TestingSolution class - user does not provide solution class
  - Value range parameter provides values for @ValueRangeProvider
  - Works with any entity/fact types via generic Object lists
```

#### of(List, List)
```
Signature: public static MoveTester<TestingSolution> of(
              List<?> entities,
              List<?> facts)
              
Parameters:
  - entities: Planning entities to include (must not be null, may be empty)
  - facts: Problem facts to include (must not be null, may be empty)
  
Returns: A new MoveTester instance with TestingSolution type

Throws:
  - IllegalArgumentException if any parameter is null
  
Preconditions:
  - All parameters must be non-null lists (can be empty)
  
Postconditions:
  - Creates a TestingSolution instance with provided entities and facts
  - No explicit value range (suitable for entity-based value ranges or list variables)
  - ScoreDirectorFactory is created automatically with dummy constraint
  - Returns a usable MoveTester<TestingSolution>
  
Implementation Notes:
  - Uses built-in TestingSolution class - user does not provide solution class
  - Suitable for moves that don't need explicit value ranges
  - For entity-based value ranges, entities themselves serve as the range
```

**Instance Methods**:

#### execute(Move)
```
Signature: public void execute(Move<Solution_> move)
              
Parameters:
  - move: The move to execute permanently (must not be null)
  
Returns: void

Throws:
  - IllegalArgumentException if move is null
  - Exception propagated from move execution
  
Preconditions:
  - Move must be non-null
  - MoveTester must not be closed
  
Postconditions:
  - Move has been executed on the solution
  - Solution state reflects the move's changes
  - Shadow variables are updated
  - Changes are permanent (no undo)
  
Side Effects:
  - If move throws exception: exception propagates to caller
  - Solution state is UNDEFINED if exception thrown (depends on where move failed)
  
Exception Handling:
  - Exceptions can be handled with try-catch at the call site
  - Or use executeTemporarily() where exceptions can be caught within the function
```
#### executeTemporarily(Move, Function)
```
Signature: public <Result_> Result_ executeTemporarily(
              Move<Solution_> move,
              Function<Solution_, Result_> function)
              
Parameters:
  - move: The move to execute temporarily (must not be null)
  - function: Function to apply while move is applied (must not be null)
  
Returns: The result returned by the function

Throws:
  - IllegalArgumentException if move is null
  - IllegalArgumentException if function is null
  - Exception propagated from move execution or function execution
  
Preconditions:
  - Move must be non-null
  - Function must be non-null
  - MoveTester must not be closed
  
Postconditions:
  - Move has been executed, then undone
  - Solution state is restored to pre-execution state
  - Shadow variables are restored
  - Function's result is returned
  
Guarantees:
  - Solution state is ALWAYS restored, even if move or function throws exception
  - Shadow variables are ALWAYS restored
  - Listeners are properly notified during undo
  - Undo happens BEFORE exception propagates
  
Exception Handling:
  - Exceptions can be handled directly within the function using try-catch
  - Function can return null or a sentinel value to indicate failure
  - Undo is guaranteed even if function throws exception
```

#### close()
```
Signature: public void close()
              
Returns: void

Throws: None

Preconditions: None
  
Postconditions:
  - MoveTester is closed
  - Resources are released
  - Subsequent operations will fail
  
Notes:
  - Idempotent - safe to call multiple times
  - Recommended usage: try-with-resources
```

---

## Behavioral Contracts

### Temporary Execution Function
- **Standard Java Function**: Uses `java.util.function.Function<Solution_, Result_>`
- **No custom interface**: No learning curve, uses familiar Java standard library
- **Function invocation**: Called exactly once per `executeTemporarily()` invocation
- **Solution parameter**: Reflects state AFTER move execution
- **Return value**: Preserved and returned from `executeTemporarily()`
- **Undo timing**: Move is undone AFTER function returns (even on exception)

**Usage Example**:
```java
String changedValue = tester.executeTemporarily(move, solution -> {
    // Move is applied here
    Entity entity = solution.getEntityList().get(0);
    return entity.getValue(); // Captured before undo
});
// Move is undone, but changedValue is still available
assertThat(changedValue).isEqualTo("expected");
```


### Exception Handling
- **No special exception handling API**: Use standard Java try-catch patterns
- **execute()**: Exceptions propagate to caller - use try-catch at call site
- **executeTemporarily()**: Exceptions can be handled within the function or propagated
- **No automatic rollback**: Move execution does NOT automatically rollback on exception (per FR-013)
- **Undo guarantee**: Temporary execution ALWAYS undoes changes, even when move or function throws exception
- **Undo timing**: For temporary execution, undo happens BEFORE exception propagates

### Shadow Variable Handling
- **Automatic initialization**: Uninitialized shadow variables are initialized before first move execution
- **Proper updates**: Shadow variables are updated via solver's standard mechanisms during move execution
- **Complete restoration**: Shadow variables are fully restored during undo in temporary execution
- **No interference**: API does not suppress or interfere with shadow variable listeners

### State Restoration Guarantees
- **Complete restoration**: `executeTemporarily()` restores ALL solution state (entities, facts, shadow variables)
- **Listener consistency**: All variable listeners are properly notified during undo
- **Idempotency**: Multiple temporary executions of same move yield same results

### Thread Safety
- **Not thread-safe**: MoveTester instances are NOT thread-safe
- **Single-threaded use**: Intended for single-threaded test scenarios
- **No sharing**: Each test should create its own MoveTester instance

### Resource Management
- **AutoCloseable**: MoveTester must be closed when done
- **Try-with-resources**: Recommended usage pattern
- **Idempotent close**: Calling close() multiple times is safe

---

## Backward Compatibility

**Stability**: This API is in the `ai.timefold.solver.test.api.move` package, indicating it is PUBLIC API with backwards compatibility guarantees.

**Versioning**: 
- Breaking changes ONLY in major versions
- Semantic versioning strictly enforced
- Deprecation policy: minimum one major version notice before removal

**Deprecation Process**:
- Clear `@Deprecated` annotation
- Javadoc with migration path
- OpenRewrite recipe provided when feasible

---

## Validation Summary

All public API methods enforce the following validations:

| Parameter | Validation | Exception | Message Format |
|-----------|------------|-----------|----------------|
| All objects | Non-null | IllegalArgumentException | "The {paramName} (%s) must not be null." |
| Lists | Non-null (empty OK) | IllegalArgumentException | "The {listName} (%s) must not be null. Maybe provide an empty list if there are no {items}." |
| ScoreDirector | Has working solution | IllegalStateException | "The scoreDirector (%s) must have a working solution set." |

All exception messages include variable names and current state per constitution requirements.

---

## Performance Characteristics

- **Permanent execution**: O(changes) where changes = number of variable modifications
- **Temporary execution**: O(changes) for execution + O(changes) for undo
- **Memory overhead**: O(changes) for recording undo information
- **Suitable for**: Testing scenarios; not optimized for production solving

---

## Dependencies

**Required**:
- `ai.timefold.solver.core.api.score.director.ScoreDirector`
- `ai.timefold.solver.core.api.score.director.ScoreDirectorFactory`
- `ai.timefold.solver.core.preview.api.move.Move`

**Optional**: None

**Visibility**:
- Uses internal classes from `ai.timefold.solver.core.impl.*` (implementation detail)
- Internal dependencies may change without notice
