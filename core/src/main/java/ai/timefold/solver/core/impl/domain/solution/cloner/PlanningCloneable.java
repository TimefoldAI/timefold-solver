package ai.timefold.solver.core.impl.domain.solution.cloner;

/**
 * An internal interface used to construct new instances of an object
 * when there is no suitable constructor.
 * Used during planning cloning to create an "empty"/"blank" instance
 * whose fields/items will be set to the planning clone of the original's
 * fields/items.
 *
 * @param <T> The type of object being cloned.
 */
public interface PlanningCloneable<T> {
    /**
     * Creates a new "empty"/"blank" instance.
     * If the {@link PlanningCloneable} is a {@link java.util.Collection}
     * or {@link java.util.Map}, the returned instance should be
     * empty and modifiable.
     *
     * @return never null, a new instance with the same type as the object being cloned.
     */
    T createNewInstance();
}
