package ai.timefold.solver.core.impl.heuristic.selector.value.chained;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.ListIterableSelector;

public interface SubChainSelector<Solution_> extends ListIterableSelector<Solution_, SubChain> {

    /**
     * @return never null
     */
    GenuineVariableDescriptor<Solution_> getVariableDescriptor();

}
