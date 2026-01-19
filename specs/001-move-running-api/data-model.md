# Data Model: Move Running API

**Feature**: Move Running API  
**Date**: January 19, 2026  
**Purpose**: Define key entities, their relationships, and state transitions

## Entity Overview

This feature introduces one new public API class (`MoveRunner`) and relies on existing solver entities (`Move`, `Solution`, `ScoreDirector`). The data model is minimal by design - this is a testing utility, not a domain model.

---

## Entities

### 1. MoveRunner<Solution_>

**Type**: Public Preview API Class  
**Package**: `ai.timefold.solver.core.preview.api.move`  
**Lifecycle**: Created → Used → Closed  
**Thread Safety**: Not thread-safe (single-threaded use only)

#### Fields

| Field | Type | Visibility | Nullability | Description |
|-------|------|------------|-------------|-------------|
| `scoreDirector` | `InnerScoreDirector<Solution_, ?>` | private | non-null | Manages solution state, shadow variables, and undo operations |

#### Methods

| Method | Return Type | Parameters | Visibility | Description |
|--------|-------------|------------|------------|-------------|
| `using(solution)` | `MoveRunner<Solution_>` | `Solution_ solution` | public static | Factory method - creates MoveRunner with initialized ScoreDirector |
| `execute(move)` | `void` | `Move<Solution_> move` | public | Executes move permanently; propagates exceptions |
| `execute(move, handler)` | `void` | `Move<Solution_> move, Consumer<Exception> exceptionHandler` | public | Executes move permanently; invokes handler and suppresses exceptions |
| `executeTemporarily(move, assertions)` | `void` | `Move<Solution_> move, Consumer<Solution_> assertions` | public | Executes move, runs assertions, then automatically undoes changes |
| `executeTemporarily(move, function)` | `<Result_> Result_` | `Move<Solution_> move, Function<Solution_, Result_> function` | public | Executes move, computes value, undoes changes, returns value |
| `close()` | `void` | - | public | Closes ScoreDirector and releases resources (from AutoCloseable) |

#### Validation Rules

- **Construction**:
  - Solution parameter MUST NOT be null → `Objects.requireNonNull(solution, "solution")`
  - ScoreDirectorFactory must be obtainable from solution → implementation detail
  
- **Execution**:
  - Move parameter MUST NOT be null → `Objects.requireNonNull(move, "move")`
  - MoveRunner must not be closed → throw `IllegalStateException` if used after close()

- **Exception Handler**:
  - If provided, handler is invoked on exception and exception propagation is suppressed
  - If not provided (execute(move) variant), exceptions propagate to caller

#### State Transitions

```
┌─────────────┐
│  Not Created│
└──────┬──────┘
       │ MoveRunner.using(solution)
       ▼
┌─────────────┐
│   Created   │◄──────┐
└──────┬──────┘       │
       │              │ execute() / executeTemporarily()
       │              │ (can be called multiple times)
       ├──────────────┘
       │
       │ close()
       ▼
┌─────────────┐
│   Closed    │ (unusable, throws IllegalStateException)
└─────────────┘
```

---

### 2. Move<Solution_> (Existing)

**Type**: Interface (solver core)  
**Package**: `ai.timefold.solver.core.preview.api.move` (or similar)  
**Role**: Represents a change operation on a planning solution

#### Key Aspects for MoveRunner

- **Method Used**: `execute(MoveDirector<Solution_, Score_>)` or equivalent
- **Behavior**: Modifies solution state via planning variable assignments
- **Side Effects**: Triggers shadow variable updates and listeners
- **Exceptions**: May throw exceptions during execution (handled by MoveRunner)

#### Relationship to MoveRunner

- MoveRunner **accepts** Move instances from users
- MoveRunner **executes** moves via ScoreDirector
- MoveRunner **does not validate** move correctness (user's responsibility)

---

### 3. Solution (Generic Type Parameter)

**Type**: User-defined class annotated with solver annotations  
**Role**: Complete state of a planning problem

#### Key Aspects for MoveRunner

- **Mutability**: Must be mutable (moves modify state)
- **Shadow Variables**: May have uninitialized shadow variables → MoveRunner initializes at construction
- **Full Solution Required**: MoveRunner requires complete solution (not just entities)

#### Relationship to MoveRunner

- Passed to `MoveRunner.using(solution)` factory method
- Used to create ScoreDirector
- Modified by move execution (permanent or temporary)
- Accessed by user assertions in `executeTemporarily()`

---

### 4. InnerScoreDirector<Solution_, Score_> (Existing, Implementation Detail)

**Type**: Interface (solver implementation)  
**Package**: `ai.timefold.solver.core.impl.score.director`  
**Role**: Internal solver component for managing solution state

#### Key Aspects for MoveRunner

- **Creation**: Created from ScoreDirectorFactory at MoveRunner construction
- **Lifecycle**: Owned by MoveRunner, closed when MoveRunner closes
- **Capabilities**:
  - Shadow variable initialization
  - Move execution
  - Undo operation support
  - Variable listener management
  - Resource cleanup

#### Relationship to MoveRunner

- MoveRunner **owns** one ScoreDirector instance
- MoveRunner **delegates** move execution to ScoreDirector
- MoveRunner **delegates** resource cleanup to ScoreDirector

---

## Relationships Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                        MoveRunner                            │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  - scoreDirector: InnerScoreDirector<Solution_, ?>   │    │
│  │  - workingSolution: Solution_                        │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  + using(solution): MoveRunner<Solution_>  [static factory]  │
│  + execute(move): void                                       │
│  + execute(move, handler): void                              │
│  + executeTemporarily(move, assertions): void                │
│  + close(): void                          [AutoCloseable]    │
└───────────┬──────────────────────┬───────────────────────────┘
            │ uses                 │ operates on
            │                      │
            ▼                      ▼
┌────────────────────────┐  ┌──────────────────┐
│ InnerScoreDirector     │  │    Solution_     │
│  (solver impl)         │  │  (user class)    │
├────────────────────────┤  ├──────────────────┤
│ - Manages state        │  │ + entities       │
│ - Handles undo         │  │ + variables      │
│ - Initializes shadows  │  │ + shadow vars    │
│ - Manages listeners    │  └──────────────────┘
└────────────────────────┘
            │
            │ executes via
            ▼
┌────────────────────────┐
│   Move<Solution_>      │
│   (user impl)          │
├────────────────────────┤
│ + execute(director)    │
│   - Modifies vars      │
│   - Triggers updates   │
└────────────────────────┘
```

---

## State Management

### Permanent Execution Flow

1. **Input**: User provides Move instance
2. **Validation**: Check move not null
3. **Execution**: `move.execute(moveDirector)`
   - Move modifies planning variables
   - ScoreDirector notifies listeners
   - Shadow variables automatically updated
4. **Exception Handling**:
   - No handler: Exception propagates to caller
   - With handler: Handler invoked, exception suppressed
5. **Result**: Solution permanently modified

### Temporary Execution Flow

1. **Input**: User provides Move and assertions callback
2. **Validation**: Check move and callback not null
3. **Execution**: `move.execute(moveDirector)`
   - Move modifies planning variables
   - ScoreDirector records undo information
   - Shadow variables automatically updated
4. **Assertions**: `assertionsCallback.accept(workingSolution)`
   - User code can inspect modified state
   - User code can throw exceptions (stops flow)
5. **Undo** (in finally block):
   - ScoreDirector restores previous state
   - Shadow variables reverted
   - Listeners notified of restoration
6. **Exception Handling**:
   - Normal flow: Solution fully restored
   - Exception in move/assertions/undo: Solution state UNDEFINED (must be discarded)
7. **Result**: Solution restored to original state (normal flow)

---

## Immutability and Mutability

| Entity | Mutability | Rationale |
|--------|------------|-----------|
| `MoveRunner` | Immutable (once created) | Holds single ScoreDirector reference; no state changes after construction |
| `Move` | Implementation-dependent | User-provided; MoveRunner doesn't care |
| `Solution` | Mutable | Modified by move execution |
| `InnerScoreDirector` | Mutable | Manages changing solution state |

---

## Memory Ownership

| Entity | Owner | Lifecycle |
|--------|-------|-----------|
| `MoveRunner` | User (caller of factory method) | Created by user, closed by user (try-with-resources) |
| `InnerScoreDirector` | MoveRunner | Created at MoveRunner construction, closed when MoveRunner closes |
| `Solution` | User | Passed to MoveRunner, modified by moves, remains owned by user |
| `Move` | User | Passed to execute methods, not retained by MoveRunner |

---

## Nullability Contract

Following JSpecify and constitution nullability policy:

### MoveRunner (annotated with @NullMarked)

- **Constructor/Factory**:
  - `solution` parameter: non-null (throws NullPointerException if null)
  - Returns: non-null MoveRunner instance
  
- **Execute Methods**:
  - `move` parameter: non-null (throws NullPointerException if null)
  - `exceptionHandler` parameter: non-null when provided
  - `assertions` parameter: non-null (throws NullPointerException if null)
  - Return: void (not applicable)
  
- **Internal Fields**:
  - `scoreDirector`: non-null (always initialized at construction)
  - `workingSolution`: non-null (always initialized at construction)

### Design Principle

**Null is internal only** - Null values must not cross class boundaries in public API. All public parameters and return values are non-null by default (via @NullMarked). No @Nullable annotations needed for this API.

---

## Validation Summary

| Validation Point | Rule | Exception Type | Implementation Pattern |
|------------------|------|----------------|------------------------|
| Factory method | solution not null | NullPointerException | `Objects.requireNonNull(solution, "solution")` |
| execute() | move not null | NullPointerException | `Objects.requireNonNull(move, "move")` |
| executeTemporarily() | move not null | NullPointerException | `Objects.requireNonNull(move, "move")` |
| executeTemporarily() | assertions not null | NullPointerException | `Objects.requireNonNull(assertions, "assertions")` |
| Any method after close() | MoveRunner not closed | IllegalStateException | Custom check in each method |

**Note**: `Objects.requireNonNull()` is used for null validation - it's concise and returns the value for chaining.

---

## Extension Points

**None by design** - MoveRunner is a sealed implementation (not extensible). Users interact via the public API but cannot subclass or extend behavior.

**Rationale**: Simplicity and clarity. Testing utility doesn't need extension points.

---

## Dependencies on Existing Solver Components

| Component | Purpose in MoveRunner | Package |
|-----------|----------------------|---------|
| `ScoreDirectorFactory` | Create ScoreDirector instances | `core.impl.score.director` |
| `InnerScoreDirector` | Execute moves, manage state, handle undo | `core.impl.score.director` |
| `Move` | User-provided change operation | `core.api.domain.move` (or similar) |
| `@NullMarked` | Nullability annotation | `org.jspecify.annotations` |

---

## Summary

The data model is intentionally minimal:
- **One new class**: `MoveRunner` (public API)
- **Leverages existing solver infrastructure**: ScoreDirector, Move, Solution
- **No new domain entities**: This is a utility, not a domain model
- **Clear ownership**: MoveRunner owns ScoreDirector, user owns Solution and Move
- **Strong contracts**: Non-null by default, clear validation rules, detailed error messages
