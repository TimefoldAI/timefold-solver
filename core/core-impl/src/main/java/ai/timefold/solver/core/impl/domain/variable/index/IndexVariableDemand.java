package ai.timefold.solver.core.impl.domain.variable.index;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class IndexVariableDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, IndexVariableSupply> {

    public IndexVariableDemand(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        super(sourceVariableDescriptor);
    }

    // ************************************************************************
    // Creation method
    // ************************************************************************

    @Override
    public IndexVariableSupply createExternalizedSupply(SupplyManager supplyManager) {
        return supplyManager.demand(((ListVariableDescriptor<Solution_>) variableDescriptor).getStateDemand());
    }

}
