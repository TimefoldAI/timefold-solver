# Feature Specification: Move Running API

**Feature Branch**: `001-move-running-api`  
**Created**: January 15, 2026  
**Status**: Preview - API subject to evolution with migration support (OpenRewrite recipes)  
**Input**: User description: "API to verify implementations of the Move interface. The tool should accept a move implementation from the user, as well as a planning solution instance to run the move on. It should perform the move, which will update the solution in the process. It should also allow to run the move temporarily - undoing the operation after the user did some checks that the operation resulted in the intended behavior."

## Clarifications

### Session 2026-01-15

- Q: When a Move is executed temporarily and then undone, how should the API handle shadow variables that were automatically updated during the move? → A: Shadow variables are automatically reverted as part of the undo operation
- Q: When a Move throws an exception during execution (either in permanent or temporary mode), what should the API do with the partially modified solution state? → A: Allow user to provide an exception handler when supplying the move; no automatic rollback on exception (user controls failure handling)
- Q: When the API restores the solution state after temporary execution, how should it handle planning variables that have registered listeners or change notifications? → A: Do not interfere with underlying mechanisms; allow all listeners, shadow variable updates, and subsystems to operate naturally during both move execution and undo
- Q: How should the system handle a Move that modifies solution state in unexpected ways (e.g., creates new entities)? → A: Allow the move to execute naturally without interference; user is responsible for verifying expected behavior
- Q: What occurs if a Move is executed on a null or invalid solution? → A: Validate null inputs upfront and throw NullPointerException; allow underlying systems to handle invalid (non-null) solutions without API interference
- Q: How are circular references or complex object graphs handled during undo operations? → A: Leverage solver's existing undo mechanisms which already handle complex object graphs correctly
- Q: What happens when the planning solution input has uninitialized shadow variables? → A: The solver's underlying architecture initializes shadow variables at MoveRunner construction time (once per MoveRunner instance); this is handled automatically by the underlying solver mechanisms

### Session 2026-01-19

- Q: Should the API support executing moves on entities without a full solution instance? → A: No. A full planning solution instance must always be provided. Entity value ranges and other constraints prevent the creation of a completely dummy solution, making this approach infeasible
- Q: When testing complex scenarios, developers might execute a move temporarily and then attempt to execute another move temporarily within that scope (nested temporary execution). How should the API handle this? → A: Document that nesting these API calls is not supported; no runtime enforcement (the API should not be concerned with what the move does under the hood)
- Q: The spec mentions the API is "intended for testing and development use cases, not production solving workflows." Should the API actively prevent or detect production use? → A: Document clearly as testing-only; no runtime enforcement (developer responsibility). Leave options open for future enforcement if needed
- Q: The spec mentions that the API "MUST accept a Move implementation instance" and execute it, but does not specify the concrete API surface that developers will use. What API pattern should be used? → A: Builder pattern with method chaining (e.g., `MoveRunner.using(solution).execute(move)` for permanent, `MoveRunner.using(solution).executeTemporarily(move, assertions -> { ... })`)
- Q: When developers test complex moves that involve multiple planning variables, shadow variables, and cascading updates, debugging failures can be challenging. Should the API provide observability/debugging support? → A: No specific logging from the API; the solver will log what it will through its existing mechanisms
- Q: The spec requires that temporary move execution must "restore the solution to its exact pre-execution state," but provides no guidance on acceptable restoration time. Should there be a performance constraint on undo operations? → A: No specific performance constraint - undo completes when it completes
- Q: The spec states "System MUST allow developers to optionally provide an exception handler when supplying a Move" (FR-001a), but with the builder pattern choice, it's unclear how the exception handler is integrated into the API flow. → A: Exception handler is a method parameter: `execute(move, exceptionHandler)`; temporary execution doesn't need an exception handler parameter as exceptions can be handled within the assertions callback
- Q: The spec states "Move execution is synchronous" but doesn't specify whether developers can safely use the MoveRunner API from multiple test threads concurrently (e.g., parallel test execution). What are the thread safety guarantees? → A: Not thread-safe - designed for single-threaded use only; each thread should create its own instance
- Q: The spec mentions that MoveRunner implements AutoCloseable and should be used with try-with-resources (FR-016, FR-017), but it doesn't specify what resources need cleanup or what happens if the runner is not properly closed. → A: Resources leak if not closed (e.g., score director, solver engine state remain active); mandatory try-with-resources. The runner holds references to score director and similar resources that require explicit cleanup
- Q: The spec mentions that when a Move throws an exception during execution, the user can provide an exception handler (FR-001a, FR-013). However, it doesn't specify whether the exception handler should be invoked before the exception is propagated to the caller, or if providing a handler prevents propagation entirely. → A: Handler is invoked and exception propagation is suppressed (caller continues normally)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Execute Move on Solution (Priority: P1)

A developer implements a custom Move and wants to execute it on a planning solution to verify correct behavior. They provide their Move implementation and a sample planning solution, execute the move via the API, and then use their own test assertions to verify the solution was modified as expected.

**Why this priority**: This is the core value proposition - enabling developers to execute moves in a controlled way so they can validate behavior using their own test assertions. Without this, the API provides no value.

**Independent Test**: Can be fully tested by providing a simple swap move on a basic planning problem, executing it via the API, and using standard assertions (outside this API) to verify that two entities exchanged their planning variable values.

**Acceptance Scenarios**:

1. **Given** a valid Move implementation and a planning solution instance, **When** the developer executes the move via the API, **Then** the move's execute() method is invoked on the solution
2. **Given** a Move that swaps two entities' positions, **When** executed via the API, **Then** the move modifies the solution's planning variables
3. **Given** a Move that assigns a value to an unassigned variable, **When** executed via the API, **Then** the move updates the solution state
4. **Given** an executed move, **When** the developer uses their own assertions to inspect the solution, **Then** all changes made by the move are present

---

### User Story 2 - Temporary Move Execution with Undo (Priority: P2)

A developer wants to test whether a Move produces the expected intermediate state without permanently modifying their solution. They execute the move in temporary mode, use their own test assertions to validate the modified state, and the API automatically reverts the solution to its original state.

**Why this priority**: This enables safe experimentation and validation without side effects, which is critical for testing and debugging custom moves in complex scenarios.

**Independent Test**: Can be tested independently by executing a move temporarily, using assertions to verify changes are applied during the temporary scope, and confirming the solution is restored to its original state afterward (using assertions before and after).

**Acceptance Scenarios**:

1. **Given** a Move and solution, **When** executed in temporary mode, **Then** the solution is modified during the temporary scope and restored afterward
2. **Given** a temporary move execution, **When** the developer uses assertions during execution, **Then** they can verify the move's changes were applied
3. **Given** a temporary move execution completes, **When** the developer uses assertions after completion, **Then** the solution state matches the pre-execution state
4. **Given** a complex move affecting multiple entities, **When** executed temporarily, **Then** all changes are fully reverted including cascade effects

---


## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept a Move implementation instance from the developer
- **FR-001a**: System MUST allow developers to optionally provide an exception handler (Consumer) as a method parameter `execute(move, exceptionHandler)` for permanent move execution; temporary execution does not require exception handler as exceptions can be handled within the assertions callback
- **FR-001b**: System MUST throw an exception if null Move or null solution are provided
- **FR-001c**: System MUST expose a builder pattern API using method chaining (e.g., `MoveRunner.using(solution).execute(move)` for permanent execution, `MoveRunner.using(solution).executeTemporarily(move, assertions -> { ... })` for temporary execution)
- **FR-002**: System MUST accept a planning solution instance to execute the move against
- **FR-003**: System MUST execute the provided Move's execute() method on the solution
- **FR-004**: System MUST support permanent move execution (changes persist after execution)
- **FR-005**: System MUST provide a mechanism to execute moves in temporary mode
- **FR-006**: System MUST automatically undo move changes when temporary execution completes normally (no exceptions), leveraging the solver's existing undo mechanisms
- **FR-007**: System MUST restore the solution to its exact pre-execution state after temporary mode completes normally (no exceptions), using the solver's built-in state management
- **FR-007a**: When exceptions are thrown during move execution, function execution, or undo itself, solution state is UNDEFINED and no restoration will be attempted
- **FR-008**: System MUST allow user code to run during temporary execution (for user assertions)
- **FR-009**: System MUST handle moves that affect multiple planning variables or entities
- **FR-010**: System MUST maintain solution integrity during undo operations in normal flow (no exceptions) - no partial rollbacks, including automatic reversion of shadow variables
- **FR-011**: System MUST support moves on solutions with both basic and list variables
- **FR-012**: System MUST trigger shadow variable updates when moves modify source variables
- **FR-012a**: System MUST NOT suppress or interfere with listeners, shadow variable updates, or other solver subsystems during move execution or undo operations
- **FR-012b**: System MUST initialize uninitialized shadow variables in input solutions using the solver's underlying mechanisms at MoveRunner construction time (once per MoveRunner instance); this is handled automatically by the underlying solver architecture
- **FR-013**: System MUST propagate exceptions thrown during move execution to the caller if no exception handler is provided; when an exception handler is provided as a method parameter, it is invoked and exception propagation is suppressed (caller continues normally); no automatic rollback on exception in either case
- **FR-014**: System MUST NOT provide solution inspection or assertion methods (user's responsibility)
- **FR-015**: System MUST NOT add API-specific logging; debugging visibility is provided by the solver's existing logging mechanisms
- **FR-016**: System MUST implement AutoCloseable for proper resource cleanup; resources (e.g., score director, solver engine state) will leak if not closed
- **FR-017**: System MUST be used with try-with-resources pattern to prevent resource leaks

### Non-Functional Requirements

- **NFR-001**: System MUST NOT be thread-safe (designed for single-threaded test use)
- **NFR-002**: System SHOULD document that each test thread must create its own instance for parallel test execution
- **NFR-003**: System SHOULD document that nesting temporary execution API calls is not supported; no runtime enforcement required

### Key Entities

- **MoveRunner**: Entry point API class providing a fluent API for move execution. Constructed via static factory method `MoveRunner.using(solution)`, returning a runner instance that supports `execute(move)` for permanent execution, `execute(move, exceptionHandler)` for permanent execution with exception handling (handler is invoked and exceptions are suppressed), and `executeTemporarily(move, function)` for temporary execution with automatic undo. Exception handling for temporary execution is performed within the function callback. Shadow variables in the solution are initialized at construction time by the underlying solver architecture. Implements AutoCloseable for proper resource cleanup; MUST be used with try-with-resources to prevent resource leaks (holds references to score director and solver engine state that require explicit cleanup).
- **Move**: Represents a change operation to be applied to the planning solution. Contains logic to modify one or more planning variables via its execute(MutableSolutionView) method. The API accepts Move instances from users.
- **Planning Solution**: The complete state of a planning problem, including all entities and their planning variable assignments. Modified by Move execution. Inspection and validation is performed by user code, not this API. A full planning solution instance is always required for move execution.
- **Planning Entity**: An object with planning variables that can be modified by moves.
- **Planning Variable**: A field within an entity that can be modified by moves. May be basic (single value) or list (ordered collection).
- **Temporary Execution Scope**: A context in which moves are executed and automatically undone. Users can run their own assertions within this scope before automatic undo occurs.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can execute a custom Move and verify changes using their own assertions
- **SC-002**: Temporary move execution completes with solution fully restored to original state in 100% of test cases where no exceptions are thrown (normal flow)
- **SC-002a**: When exceptions are thrown during temporary execution, NO undo is performed and the solution is left in a corrupted state - developers must discard the solution instance
- **SC-003**: Developers can perform complete validation workflows (execute move, run assertions, automatic undo) without manual state management in normal flow
- **SC-004**: Move execution workflow completes without requiring developers to understand internal undo mechanisms
- **SC-005**: API supports all standard Move types used in the solver without type-specific handling
- **SC-006**: User assertions can be executed during temporary scope before automatic undo
- **SC-007**: API is used to test at least 2 different solver move types (dogfooding validates real-world usefulness)

## Assumptions

- Moves follow standard solver conventions (they modify solution state through planning variable assignments via execute(MutableSolutionView))
- Solutions are mutable and their state can be captured and restored
- Input solutions may have uninitialized shadow variables; the solver's underlying architecture will initialize them at MoveRunner construction time (once per MoveRunner instance)
- Developers have access to both Move implementations and sample solution instances for testing
- A full planning solution instance must always be provided; entity value ranges and other constraints prevent the creation of a completely dummy solution
- Move execution is synchronous (completes before returning control to caller)
- MoveRunner API is not thread-safe; designed for single-threaded use only (each thread should create its own instance for parallel test execution) - See NFR-001
- Undo operations have no specific performance constraints; restoration time depends on solution complexity and underlying solver mechanisms
- Performance characteristics: O(changes) for execution and undo, where changes = number of variable modifications
- Memory overhead: O(changes) for recording undo information
- API is optimized for correctness in testing scenarios, not production performance
- The API is intended for testing and development use cases, not production solving workflows; this will be clearly documented without runtime enforcement (developer responsibility)
- Solution inspection and assertion logic is provided by user test code, not this API
- Users have access to the solution object before, during (in temporary scope), and after move execution to run their own validations
