package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

/**
 * Only selects values from the child value selector that are initialized.
 * This is used for {@link ElementDestinationSelector}’s child value selector during Construction Heuristic phase
 * to filter out values which cannot be used to build a destination {@link PositionInList}.
 */
public final class AssignedListValueSelector<Solution_> extends AbstractInverseEntityFilteringValueSelector<Solution_> {

    public AssignedListValueSelector(EntityIndependentValueSelector<Solution_> childValueSelector) {
        super(childValueSelector);
    }

    @Override
    protected boolean valueFilter(Object value) {
        if (listVariableStateSupply.getUnassignedCount() == 0) {
            return true; // Avoid hash lookup.
        }
        return listVariableStateSupply.isAssigned(value);
    }

    @Override
    public String toString() {
        return "Assigned(" + childValueSelector + ")";
    }
}
