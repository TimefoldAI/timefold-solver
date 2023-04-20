package ai.timefold.solver.core.impl.heuristic.selector.value;

import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;

/**
 * @see FromSolutionPropertyValueSelector
 */
public interface EntityIndependentValueSelector<Solution_> extends ValueSelector<Solution_>,
        IterableSelector<Solution_, Object> {

}
