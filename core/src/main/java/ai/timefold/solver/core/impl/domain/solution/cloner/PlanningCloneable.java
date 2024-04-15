package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.util.Collection;
import java.util.Map;

/**
 * Used to construct new instances of an object when there is no suitable constructor.
 * Used during planning cloning to create an "empty"/"blank" instance
 * whose fields/items will be set to the planning clone of the original's fields/items.
 * <p>
 * This interface is internal.
 * Do not use it in user code.
 *
 * @param <T> The type of object being cloned.
 */
public interface PlanningCloneable<T> {

    /**
     * Creates a new "empty"/"blank" instance.
     * If the {@link PlanningCloneable} is a {@link Collection} or {@link Map},
     * the returned instance should be empty and modifiable.
     *
     * @return never null, a new instance with the same type as the object being cloned.
     */
    T createNewInstance();

}
