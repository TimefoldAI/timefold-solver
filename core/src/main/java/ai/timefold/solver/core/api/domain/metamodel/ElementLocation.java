package ai.timefold.solver.core.api.domain.metamodel;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

/**
 * A supertype for {@link LocationInList} and {@link UnassignedLocation}.
 * <p>
 * {@link PlanningListVariable#allowsUnassignedValues()} introduces {@link UnassignedLocation},
 * and the handling of unassigned values is brittle.
 * These values may leak into code which expects {@link LocationInList} instead.
 * Therefore, we use {@link ElementLocation} and we force calling code to cast to either of the two subtypes.
 * This prevents accidental use of {@link UnassignedLocation} in places where {@link LocationInList} is expected,
 * catching this error as early as possible.
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

    static UnassignedLocation unassigned() {
        return DefaultUnassignedLocation.INSTANCE;
    }

    default <Entity_> LocationInList<Entity_> ensureAssigned() {
        return ensureAssigned(() -> "Unexpected unassigned location.");
    }

    @SuppressWarnings("unchecked")
    default <Entity_> LocationInList<Entity_> ensureAssigned(Supplier<String> messageSupplier) {
        if (this instanceof LocationInList<?> locationInList) {
            return (LocationInList<Entity_>) locationInList;
        } else {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

}
