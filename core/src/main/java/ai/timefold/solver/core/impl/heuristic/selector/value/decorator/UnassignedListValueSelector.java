package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

/**
 * Only selects values from the child value selector that are uninitialized.
 * This used for {@link QueuedValuePlacer}’s recording value selector during Construction Heuristic phase
 * to prevent reinitializing values.
 */
public final class UnassignedListValueSelector<Solution_> extends AbstractInverseEntityFilteringValueSelector<Solution_> {

    public UnassignedListValueSelector(EntityIndependentValueSelector<Solution_> childValueSelector) {
        super(childValueSelector);
    }

    @Override
    protected boolean valueFilter(Object value) {
        if (listVariableStateSupply.getUnassignedCount() == 0) {
            return false; // Avoid hash lookup.
        }
        return !listVariableStateSupply.isAssigned(value);
    }

    @Override
    public String toString() {
        return "Unassigned(" + childValueSelector + ")";
    }
}
