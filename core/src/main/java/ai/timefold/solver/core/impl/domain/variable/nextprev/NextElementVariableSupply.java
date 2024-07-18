package ai.timefold.solver.core.impl.domain.variable.nextprev;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Only supported for {@link PlanningListVariable list variables}.
 * <p>
 * To get an instance, demand an {@link NextElementVariableDemand} from {@link InnerScoreDirector#getSupplyManager()}.
 */
public interface NextElementVariableSupply extends Supply {

    /**
     * Get next element in the {@link PlanningListVariable list variable} of a given planning value.
     *
     * @param planningValue never null
     * @return {@code next element} assigned to the #planningValue of the list variable,
     *         or {@code null} when the value is unassigned, or it is the last element
     */
    Object getNext(Object planningValue);
}
