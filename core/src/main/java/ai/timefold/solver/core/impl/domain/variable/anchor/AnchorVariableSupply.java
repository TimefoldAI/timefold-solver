package ai.timefold.solver.core.impl.domain.variable.anchor;

import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Only supported for chained variables.
 * <p>
 * To get an instance, demand an {@link AnchorVariableDemand} from {@link InnerScoreDirector#getSupplyManager()}.
 */
public interface AnchorVariableSupply extends Supply {

    /**
     * @param entity never null
     * @return sometimes null, the anchor for the entity
     */
    Object getAnchor(Object entity);

}
