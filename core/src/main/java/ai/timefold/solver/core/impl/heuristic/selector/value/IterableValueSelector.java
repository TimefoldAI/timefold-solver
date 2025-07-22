package ai.timefold.solver.core.impl.heuristic.selector.value;

import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.IterableFromEntityPropertyValueSelector;

/**
 * @see IterableFromSolutionPropertyValueSelector
 * @see IterableFromEntityPropertyValueSelector
 */
public interface IterableValueSelector<Solution_> extends ValueSelector<Solution_>,
        IterableSelector<Solution_, Object> {

}
