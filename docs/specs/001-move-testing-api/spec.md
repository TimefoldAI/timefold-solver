# Feature Specification: Move Testing API

**Feature Branch**: `001-move-testing-api`  
**Created**: January 15, 2026  
**Status**: Draft  
**Input**: User description: "API to verify implementations of the Move interface. The tool should accept a move implementation from the user, as well as a planning solution instance to run the move on. It should perform the move, which will update the solution in the process. It should also allow to run the move temporarily - undoing the operation after the user did some checks that the operation resulted in the intended behavior."

## Clarifications

### Session 2026-01-15

- Q: When a Move is executed temporarily and then undone, how should the API handle shadow variables that were automatically updated during the move? → A: Shadow variables are automatically reverted as part of the undo operation
- Q: When executing a Move on entities without a full solution (User Story 3), how should the API handle moves that internally call solution-level methods like `getScore()` or `getProblemFacts()`? → A: Provide a minimal mock solution object with only the provided entities and facts
- Q: When a Move throws an exception during execution (either in permanent or temporary mode), what should the API do with the partially modified solution state? → A: Allow user to provide an exception handler when supplying the move; no automatic rollback on exception (user controls failure handling)
- Q: When the API restores the solution state after temporary execution, how should it handle planning variables that have registered listeners or change notifications? → A: Do not interfere with underlying mechanisms; allow all listeners, shadow variable updates, and subsystems to operate naturally during both move execution and undo
- Q: When executing moves in temporary mode on entities without a full solution, how should the API capture and restore state? → A: Leverage the solver's existing undo mechanisms; the solver already understands how to modify solutions/entities and perform undo operations
- Q: How should the system handle a Move that modifies solution state in unexpected ways (e.g., creates new entities)? → A: Allow the move to execute naturally without interference; user is responsible for verifying expected behavior
- Q: What occurs if a Move is executed on a null or invalid solution/entities? → A: Validate null inputs upfront and throw IllegalArgumentException; allow underlying systems to handle invalid (non-null) solutions/entities without API interference
- Q: How does the system handle moves on partial entity sets (not all solution entities provided)? → A: Pass provided entities to underlying systems without completeness validation; underlying systems handle any issues and may throw exceptions caught by user's exception handler
- Q: How are circular references or complex object graphs handled during undo operations? → A: Leverage solver's existing undo mechanisms which already handle complex object graphs correctly
- Q: What happens when the planning solution input has uninitialized shadow variables? → A: The API uses the solver's underlying mechanisms to initialize shadow variables before move execution

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

### User Story 3 - Execute Move on Entities Without Full Solution (Priority: P2)

A developer wants to test a Move in isolation without constructing a complete planning solution. They provide only the specific entities and facts needed for the move, execute the move via the API, and use their own test assertions to verify the entities were modified correctly.

**Why this priority**: This simplifies testing by removing the need to construct full solution objects. Developers can focus on testing move logic in isolation with minimal setup, which is especially valuable for unit testing.

**Independent Test**: Can be tested by providing just two entity objects (without a solution wrapper), executing a swap move on them, and verifying via assertions that their planning variable values were exchanged.

**Acceptance Scenarios**:

1. **Given** a Move and relevant entities (without a full solution), **When** the developer executes the move via the API, **Then** the move's execute() method is invoked on those entities
2. **Given** entities with planning variables, **When** a move is executed on them without a solution, **Then** the entities' planning variables are modified as expected
3. **Given** a move requiring specific facts, **When** executed with just those entities and facts, **Then** the move operates correctly without needing a complete solution object
4. **Given** an executed move on entities, **When** the developer uses assertions to inspect the entities, **Then** all changes made by the move are present

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept a Move implementation instance from the developer
- **FR-001a**: System MUST allow developers to optionally provide an exception handler when supplying a Move
- **FR-001b**: System MUST throw an exception if null Move, null solution, or null entities are provided
- **FR-002**: System MUST accept a planning solution instance to execute the move against
- **FR-003**: System MUST execute the provided Move's execute() method on the solution
- **FR-004**: System MUST support permanent move execution (changes persist after execution)
- **FR-005**: System MUST provide a mechanism to execute moves in temporary mode
- **FR-006**: System MUST automatically undo move changes when temporary execution completes, leveraging the solver's existing undo mechanisms
- **FR-007**: System MUST restore the solution to its exact pre-execution state after temporary mode, using the solver's built-in state management
- **FR-008**: System MUST allow user code to run during temporary execution (for user assertions)
- **FR-009**: System MUST handle moves that affect multiple planning variables or entities
- **FR-010**: System MUST maintain solution integrity during undo operations (no partial rollbacks), including automatic reversion of shadow variables
- **FR-011**: System MUST support moves on solutions with both basic and list variables
- **FR-012**: System MUST trigger shadow variable updates when moves modify source variables
- **FR-012a**: System MUST NOT suppress or interfere with listeners, shadow variable updates, or other solver subsystems during move execution or undo operations
- **FR-012b**: System MUST initialize uninitialized shadow variables in input solutions using the solver's underlying mechanisms before move execution
- **FR-013**: System MUST propagate exceptions thrown during move execution to the user-provided exception handler (if provided) or to the caller; no automatic rollback on exception
- **FR-014**: System MUST NOT provide solution inspection or assertion methods (user's responsibility)
- **FR-015**: System MUST accept entities and facts instead of a full solution for move execution
- **FR-016**: System MUST execute moves on provided entities by creating a minimal mock solution object containing only those entities and facts
- **FR-017**: System MUST support temporary mode for moves executed on entities without a solution
- **FR-018**: System MUST restore entity state after temporary execution on entities without a solution, using the solver's existing undo mechanisms

### Key Entities

- **Move**: Represents a change operation to be applied to the planning solution. Contains logic to modify one or more planning variables via its execute(MutableSolutionView) method. The API accepts Move instances from users.
- **Planning Solution**: The complete state of a planning problem, including all entities and their planning variable assignments. Modified by Move execution. Inspection and validation is performed by user code, not this API. Optional - moves can be executed without a full solution.
- **Planning Entity**: An object with planning variables that can be modified by moves. Can be provided individually without a full solution for isolated testing.
- **Planning Variable**: A field within an entity that can be modified by moves. May be basic (single value) or list (ordered collection).
- **Planning Facts**: Problem facts that may be needed by moves (e.g., resources, constraints data). Can be provided alongside entities for move execution without a full solution.
- **Temporary Execution Scope**: A context in which moves are executed and automatically undone. Users can run their own assertions within this scope before automatic undo occurs. Works with both full solutions and individual entities.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can execute a custom Move and verify changes using their own assertions
- **SC-002**: Temporary move execution completes with solution fully restored to original state in 100% of test cases
- **SC-003**: Developers can perform complete validation workflows (execute move, run assertions, automatic undo) without manual state management
- **SC-004**: Move execution workflow completes without requiring developers to understand internal undo mechanisms
- **SC-005**: API supports all standard Move types used in the solver without type-specific handling
- **SC-006**: User assertions can be executed during temporary scope before automatic undo

## Assumptions

- Moves follow standard solver conventions (they modify solution state through planning variable assignments via execute(MutableSolutionView))
- Solutions are mutable and their state can be captured and restored
- Input solutions may have uninitialized shadow variables; the API will initialize them using the solver's mechanisms before move execution
- Entities are mutable and their state can be captured and restored independently of a full solution
- Developers have access to both Move implementations and sample solution instances for testing
- Developers can provide individual entities and facts without constructing a complete solution when testing moves in isolation
- Move execution is synchronous (completes before returning control to caller)
- The API is intended for testing and development use cases, not production solving workflows
- Solution inspection and assertion logic is provided by user test code, not this API
- Users have access to the solution object (or entities when no solution provided) before, during (in temporary scope), and after move execution to run their own validations
- When executing moves on entities without a full solution, a minimal mock solution object is created containing only the provided entities and facts
