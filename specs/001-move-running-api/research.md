# Research: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026 (Final)  
**Purpose**: Technical discovery and design decisions for implementing a move testing utility

## Key Discovery: Using InnerScoreDirector

### Critical Finding

The solver's `InnerScoreDirector` interface provides almost everything we need:

1. **InnerScoreDirector** currently has:
   - `executeMove(Move<Solution_> move)` - Permanent move execution ✅
   - `executeTemporaryMove(Move<Solution_> move, boolean assertFromScratch)` - Temporary with automatic undo (returns score, no callback)
   - `setWorkingSolution(Solution_)` - Initializes shadow variables ✅
   - `close()` - Resource cleanup ✅

2. **We need to add** one method to InnerScoreDirector:
   - `void executeTemporaryMove(Move<Solution_>, Consumer<Solution_>)` - Temporary with callback for user assertions

**Decision**: Use InnerScoreDirector exclusively, add new callback method

**Rationale**: 
- ✅ InnerScoreDirector is the right abstraction for move execution
- ✅ New method provides callback hook for user assertions
- ✅ Clean API - MoveRunner only depends on InnerScoreDirector
- ✅ Automatic undo handled internally by InnerScoreDirector

---

## Research Questions Resolved

### 1. Permanent Move Execution

**Question**: How to execute moves permanently?

**Decision**: Use `InnerScoreDirector.executeMove()` directly
```java
public void execute(Move<Solution_> move) {
    scoreDirector.executeMove(move);
}
```

**Rationale**:
- ✅ Direct API - no wrapper needed
- ✅ Handles shadow variable updates automatically
- ✅ Triggers variable listeners
- ✅ Simple and straightforward

---

### 2. Temporary Move Execution

**Question**: How to execute moves temporarily with user assertions?

**Problem**: Existing `InnerScoreDirector.executeTemporaryMove(move, boolean)` doesn't provide a callback hook - it just returns the score after automatically undoing.

**Solution**: Add new method to `InnerScoreDirector` interface:

```java
// New method to add to InnerScoreDirector interface
<Result_> Result_ executeTemporaryMove(Move<Solution_> move, Function<Solution_, Result_> callback);
```

**Implementation in AbstractScoreDirector** (uses internal move director):
```java
@Override
public <Result_> Result_ executeTemporaryMove(Move<Solution_> move, Function<Solution_, Result_> callback) {
    // Implementation detail: delegates to internal move director's executeTemporary with callback
    return moveDirector.executeTemporary(move, (score, undoMove) -> {
        return callback.apply(workingSolution);
    });
    // Undo happens automatically via move director's infrastructure
}
```

**MoveRunner usage**:
```java
// Void variant - for assertions
public void executeTemporarily(Move<Solution_> move, Consumer<Solution_> assertions) {
    scoreDirector.executeTemporaryMove(move, solution -> {
        assertions.accept(solution);
        return null;
    });
}

// Return value variant - for computations
public <Result_> Result_ executeTemporarily(Move<Solution_> move, Function<Solution_, Result_> function) {
    return scoreDirector.executeTemporaryMove(move, function);
}
```

**Rationale**:
- ✅ Maintains encapsulation - MoveRunner only knows about InnerScoreDirector
- ✅ Generic `Function<Solution_, Result_>` allows returning any type
- ✅ Implementation detail: AbstractScoreDirector uses internal move director
- ✅ Both void (Consumer) and return value (Function) use cases supported - no knowledge of implementation details

---

### 3. Shadow Variable Initialization

**Question**: When and how are shadow variables initialized?

**Decision**: Call `setWorkingSolution()` at construction
```java
public static <Solution_> MoveRunner<Solution_> on(Solution_ solution) {
    // ... create scoreDirector ...
    scoreDirector.setWorkingSolution(solution); // Initializes shadow variables
    return new MoveRunner<>(solution, scoreDirector);
}
```

**Rationale**:
- ✅ One-time initialization at construction
- ✅ Automatic via InnerScoreDirector
- ✅ Satisfies FR-012b

---

### 4. Exception Handling

**Question**: How to provide optional exception handling?

**Decision**: Method overloading with try-catch
```java
public void execute(Move<Solution_> move) {
    Objects.requireNonNull(move, "move");
    scoreDirector.executeMove(move); // Propagates exceptions
}

public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler) {
    Objects.requireNonNull(move, "move");
    Objects.requireNonNull(exceptionHandler, "exceptionHandler");
    try {
        scoreDirector.executeMove(move);
    } catch (Exception e) {
        exceptionHandler.accept(e);
        // Exception suppressed
    }
}
```

**Rationale**:
- ✅ Simple wrapper
- ✅ Clear semantics: no handler = propagate, with handler = suppress
- ✅ Standard Java pattern

---

### 5. Resource Management

**Question**: What needs cleanup?

**Decision**: Close InnerScoreDirector
```java
@Override
public void close() throws Exception {
    scoreDirector.close();
}
```

**Rationale**:
- ✅ InnerScoreDirector is AutoCloseable
- ✅ Handles score calculation engine, listeners, etc.
- ✅ Simple delegation

---

### 6. Bootstrap: Creating InnerScoreDirector

**Question**: How to create InnerScoreDirector from a solution?

**Decision**: Use ConstraintStreamScoreDirectorFactory with a single dummy constraint

**Approach**:
```java
public static <Solution_> MoveRunner<Solution_> on(Solution_ solution) {
    // 1. Get SolutionDescriptor from solution class
    SolutionDescriptor<Solution_> solutionDescriptor = 
        SolutionDescriptor.buildSolutionDescriptor(solution.getClass());
    
    // 2. Create ConstraintStreamScoreDirectorFactory with dummy constraint
    //    (Must have at least one constraint - empty array will fail)
    var scoreDirectorFactory = new ConstraintStreamScoreDirectorFactory<>(
        solutionDescriptor,
        (constraintFactory) -> {
            // Create a single dummy constraint using first entity from solution descriptor
            var entityDescriptors = solutionDescriptor.getEntityDescriptorList();
            if (entityDescriptors.isEmpty()) {
                throw new IllegalStateException(
                    "The solution (%s) has no planning entities.".formatted(solution));
            }
            var firstEntityClass = entityDescriptors.get(0).getEntityClass();
            
            return new Constraint[] {
                constraintFactory.forEach(firstEntityClass)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Dummy constraint for MoveRunner initialization")
            };
        }
    );
    
    // 3. Build InnerScoreDirector
    var scoreDirector = (InnerScoreDirector<Solution_, ?>) 
        scoreDirectorFactory.buildScoreDirector(false, false);
    
    // 4. Initialize shadow variables
    scoreDirector.setWorkingSolution(solution);
    
    // 5. Return MoveRunner
    return new MoveRunner<>(solution, scoreDirector);
}
```

**Rationale**:
- ✅ Proper initialization without mocking
- ✅ No encapsulation breaking
- ✅ ConstraintStreamScoreDirectorFactory requires at least one constraint
- ✅ Single dummy constraint sufficient - uses first entity from solution descriptor
- ✅ Constraint is never evaluated - we don't calculate scores, just need infrastructure
- ✅ Shadow variables initialized via setWorkingSolution()

**Why this approach**:
- Constraints are not important for MoveRunner - we only execute moves and verify state changes
- We need a properly initialized ScoreDirector for shadow variable handling and undo mechanisms
- ConstraintStreamScoreDirectorFactory requires at least one constraint (empty array fails)
- We create a simple constraint that iterates over the first entity type and penalizes with SimpleScore.ONE
- This constraint is never actually evaluated - we're not calculating scores, just initializing infrastructure

---

### 7. API Design

**Decision**: Static factory + instance methods
```java
public static <Solution_> MoveRunner<Solution_> using(Solution_ solution)

public void execute(Move<Solution_> move)
public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler)
public void executeTemporarily(Move<Solution_> move, Consumer<Solution_> assertions)
public <Result_> Result_ executeTemporarily(Move<Solution_> move, Function<Solution_, Result_> function)

@Override
public void close() throws Exception
```

**Rationale**:
- ✅ Fluent: `MoveRunner.using(solution).execute(move)`
- ✅ Type-safe generic capture
- ✅ Minimal API surface
- ✅ Two executeTemporarily overloads: void (for assertions) and with return value (for computations)
- ✅ Standard AutoCloseable pattern

---

## Implementation Architecture

### Class Structure

```java
@NullMarked
public final class MoveRunner<Solution_> implements AutoCloseable {
    
    private final Solution_ workingSolution;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private boolean closed = false;
    
    private MoveRunner(Solution_ solution, InnerScoreDirector<Solution_, ?> scoreDirector) {
        this.workingSolution = solution;
        this.scoreDirector = scoreDirector;
    }
    
    public static <Solution_> MoveRunner<Solution_> using(Solution_ solution) {
        // Validate input
        Objects.requireNonNull(solution, "solution");
        
        // Get solution descriptor
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(solution.getClass());
        
        // Create score director factory with dummy constraint
        var scoreDirectorFactory = new ConstraintStreamScoreDirectorFactory<>(
            solutionDescriptor,
            (constraintFactory) -> {
                var entityDescriptors = solutionDescriptor.getEntityDescriptorList();
                if (entityDescriptors.isEmpty()) {
                    throw new IllegalStateException(
                        "The solution (%s) has no planning entities.".formatted(solution));
                }
                var firstEntityClass = entityDescriptors.get(0).getEntityClass();
                
                return new Constraint[] {
                    constraintFactory.forEach(firstEntityClass)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Dummy constraint for MoveRunner initialization")
                };
            }
        );
        
        // Build and initialize score director
        var scoreDirector = (InnerScoreDirector<Solution_, ?>) 
            scoreDirectorFactory.buildScoreDirector(false, false);
        scoreDirector.setWorkingSolution(solution);
        
        return new MoveRunner<>(solution, scoreDirector);
    }
    
    public void execute(Move<Solution_> move) {
        validateNotClosed();
        Objects.requireNonNull(move, "move");
        scoreDirector.executeMove(move);
    }
    
    public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler) {
        validateNotClosed();
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(exceptionHandler, "exceptionHandler");
        try {
            scoreDirector.executeMove(move);
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }
    
    public void executeTemporarily(Move<Solution_> move, Consumer<Solution_> assertions) {
        validateNotClosed();
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(assertions, "assertions");
        
        // Use new InnerScoreDirector method with callback
        scoreDirector.executeTemporaryMove(move, assertions);
        // Undo happens automatically inside executeTemporaryMove
    }
    
    public <Result_> Result_ executeTemporarily(Move<Solution_> move, Function<Solution_, Result_> function) {
        validateNotClosed();
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(function, "function");
        
        // Use new InnerScoreDirector method with callback that returns a value
        return scoreDirector.executeTemporaryMove(move, solution -> function.apply(solution));
        // Undo happens automatically inside executeTemporaryMove
    }
    
    @Override
    public void close() throws Exception {
        if (!closed) {
            scoreDirector.close();
            closed = true;
        }
    }
}
```

---

## Technology Decisions Summary

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **Permanent Execution** | InnerScoreDirector.executeMove() | Direct, simple, handles everything |
| **Temporary Execution** | New InnerScoreDirector.executeTemporaryMove(move, callback) | Provides callback with generic return type |
| **Temporary Variants** | Function<Solution_, Result_> for return value, Consumer for void | Supports both computation and assertion use cases |
| **Shadow Variables** | setWorkingSolution() at construction | Automatic initialization |
| **Exception Handling** | Method overloads with try-catch | Standard Java pattern |
| **Resource Management** | Delegate to scoreDirector.close() | AutoCloseable |
| **Bootstrap** | ConstraintStreamScoreDirectorFactory with dummy constraint | Proper initialization, no mocking |
| **API Pattern** | Static factory + instance methods | Fluent, type-safe |

---

## Dependencies

**Required**:
- `InnerScoreDirector` - Main interface (executeMove, new executeTemporaryMove with callback, setWorkingSolution, close)
- `SolutionDescriptor` - For extracting solution metadata from solution class
- `ConstraintStreamScoreDirectorFactory` - For creating properly initialized ScoreDirector
- `Constraint` - Array of constraints (at least one required)
- `ConstraintFactory` - For building dummy constraint
- `SimpleScore` - For dummy constraint penalty value
- `Move` - User-provided moves

**No External Dependencies**: All from solver core

---

## Implementation Complexity

**LOW** - Mostly delegation after adding one new method to InnerScoreDirector:
- **New infrastructure**: Add `executeTemporaryMove(move, callback)` to InnerScoreDirector interface
- **Bootstrap**: ConstraintStreamScoreDirectorFactory with dummy constraint (straightforward)
- **Permanent**: Direct call to `scoreDirector.executeMove()`
- **Temporary**: Call new `scoreDirector.executeTemporaryMove(move, assertions)`
- **Shadow init**: Call `setWorkingSolution()`
- **Cleanup**: Call `scoreDirector.close()`

**Main tasks**: 
1. Add new method to InnerScoreDirector interface (simple signature)
2. Implement in AbstractScoreDirector (execute, callback, undo pattern)
3. Bootstrap is straightforward - ConstraintStreamScoreDirectorFactory with dummy constraint

---

## Open Questions for Implementation

1. **New InnerScoreDirector method**:
   - Add signature to InnerScoreDirector interface: `<Result_> Result_ executeTemporaryMove(Move, Function<Solution_, Result_>)`
   - Implement in AbstractScoreDirector: delegate to internal `moveDirector.executeTemporary(move, callback)`
   - Note: AbstractScoreDirector already has access to `moveDirector` field - this is an implementation detail hidden from MoveRunner
   - Generic return type supports both void (return null) and value-returning use cases

2. **ConstraintStreamScoreDirectorFactory constructor**: Verify exact signature
   - Expected: Constructor taking SolutionDescriptor and ConstraintProvider
   - Confirm parameters for buildScoreDirector() method

3. **Generic bounds**: Should `<Solution_>` have constraints?
   - Likely no bounds - solver handles validation via SolutionDescriptor

---

## References

- `InnerScoreDirector`: Interface for score director operations
- `ConstraintStreamScoreDirectorFactory`: Standard way to create ScoreDirector for Constraint Streams
- `SolutionDescriptor`: Metadata about solution classes
- Constitution: `.specify/memory/constitution.md`
- Spec: `specs/001-move-running-api/spec.md`

---

## Summary

**Correct approach**:
1. Use `InnerScoreDirector.executeMove()` for permanent execution (already exists)
2. Add `InnerScoreDirector.executeTemporaryMove(move, callback)` for temporary execution with callbacks (new method)
3. Bootstrap via `ConstraintStreamScoreDirectorFactory` with dummy constraint (no mocking)
4. MoveRunner is a thin wrapper providing user-friendly API

**Key changes needed**: 
- Add one new method to InnerScoreDirector interface
- Create MoveRunner class that uses InnerScoreDirector

**Complexity**: LOW - simple delegation with minimal logic  
**Encapsulation**: MAINTAINED - MoveRunner only depends on InnerScoreDirector
