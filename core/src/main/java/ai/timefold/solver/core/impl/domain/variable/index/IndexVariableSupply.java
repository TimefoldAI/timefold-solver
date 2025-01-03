package ai.timefold.solver.core.impl.domain.variable.index;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

/**
 * Only supported for {@link PlanningListVariable list variables}.
 */
public interface IndexVariableSupply extends Supply {

    /**
     * Get {@code planningValue}'s index in the {@link PlanningListVariable list variable} it is an element of.
     *
     * @param planningValue never null
     * @return {@code planningValue}'s index in the list variable it is an element of or {@code null} if the value is unassigned
     */
    Integer getIndex(Object planningValue);
}
