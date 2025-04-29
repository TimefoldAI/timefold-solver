package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

import org.jspecify.annotations.NullMarked;

/**
 * A supertype for {@link PositionInList} and {@link UnassignedElement}.
 * <p>
 * {@link PlanningListVariable#allowsUnassignedValues()} allows for a value to not be part of any entity's list.
 * This introduces null into user code, and makes it harder to reason about the code.
 * Therefore, we introduce {@link UnassignedElement} to represent this null value,
 * and user code must explicitly decide how to handle this case.
 * This prevents accidental use of {@link UnassignedElement} in places where {@link PositionInList} is expected,
 * catching this error as early as possible.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 */
@NullMarked
public sealed interface ElementPosition permits PositionInList, UnassignedElement {

    /**
     * Create a new instance of {@link PositionInList}.
     * User code should never need to call this method.
     *
     * @param entity Entity whose {@link PlanningListVariable} contains the value.
     * @param index 0 or higher
     * @return never null
     */
    static PositionInList of(Object entity, int index) {
        return new DefaultPositionInList(entity, index);
    }

    /**
     * Returns a singleton instance of {@link UnassignedElement}.
     * User code should never need to call this method.
     *
     * @return never null
     */
    static UnassignedElement unassigned() {
        return DefaultUnassignedElement.INSTANCE;
    }

    /**
     * Returns {@link PositionInList} if this position is assigned, otherwise throws an exception.
     *
     * @return Position of the value in an entity's {@link PlanningListVariable}.
     * @throws IllegalStateException If this position is unassigned.
     */
    default PositionInList ensureAssigned() {
        return ensureAssigned(() -> "Unexpected unassigned position.");
    }

    /**
     * Returns {@link PositionInList} if this position is assigned, otherwise throws an exception.
     *
     * @param messageSupplier The message to give the exception.
     * @return Position of the value in an entity's {@link PlanningListVariable}.
     * @throws IllegalStateException If this position is unassigned.
     */
    PositionInList ensureAssigned(Supplier<String> messageSupplier);

}
