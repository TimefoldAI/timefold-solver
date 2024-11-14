package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

/**
 * Currently only supported for chained variables and {@link PlanningListVariable list variables},
 * which guarantee that no 2 entities use the same planningValue.
 */
public interface SingletonInverseVariableSupply extends Supply {

    /**
     * If entity1.varA = x then the inverse of x is entity1.
     *
     * @param planningValue never null
     * @return sometimes null, an entity for which the planning variable is the planningValue.
     */
    Object getInverseSingleton(Object planningValue);

}
