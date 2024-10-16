package ai.timefold.solver.core.api.domain.metamodel;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

/**
 * A supertype for {@link LocationInList} and {@link UnassignedLocation}.
 * <p>
 * {@link PlanningListVariable#allowsUnassignedValues()} allows for a value to not be part of any entity's list.
 * This introduces null into user code, and makes it harder to reason about the code.
 * Therefore we introduce {@link UnassignedLocation} to represent this null value,
 * and user code must explicitly decide how to handle this case.
 * This prevents accidental use of {@link UnassignedLocation} in places where {@link LocationInList} is expected,
 * catching this error as early as possible.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 */
public sealed interface ElementLocation permits LocationInList, UnassignedLocation {

    /**
     * Create a new instance of {@link LocationInList}.
     * User code should never need to call this method.
     *
     * @param entity never null
     * @param index 0 or higher
     * @return never null
     */
    static <Entity_> LocationInList<Entity_> of(Entity_ entity, int index) {
        return new DefaultLocationInList<>(entity, index);
    }

    /**
     * Returns a singleton instance of {@link UnassignedLocation}.
     * User code should never need to call this method.
     *
     * @return never null
     */
    static UnassignedLocation unassigned() {
        return DefaultUnassignedLocation.INSTANCE;
    }

    /**
     * Returns {@link LocationInList} if this location is assigned, otherwise throws an exception.
     *
     * @return never null
     * @throws IllegalStateException if this location is unassigned
     */
    default <Entity_> LocationInList<Entity_> ensureAssigned() {
        return ensureAssigned(() -> "Unexpected unassigned location.");
    }

    /**
     * Returns {@link LocationInList} if this location is assigned, otherwise throws an exception.
     *
     * @param messageSupplier never null, the message to give the exception
     * @return never null
     * @throws IllegalStateException if this location is unassigned
     */
    @SuppressWarnings("unchecked")
    default <Entity_> LocationInList<Entity_> ensureAssigned(Supplier<String> messageSupplier) {
        if (this instanceof LocationInList<?> locationInList) {
            return (LocationInList<Entity_>) locationInList;
        } else {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

}
