package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class SingletonListInverseVariableDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, SingletonInverseVariableSupply> {

    public SingletonListInverseVariableDemand(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        super(sourceVariableDescriptor);
    }

    // ************************************************************************
    // Creation method
    // ************************************************************************

    @Override
    public SingletonInverseVariableSupply createExternalizedSupply(SupplyManager supplyManager) {
        return supplyManager.demand(((ListVariableDescriptor<Solution_>) variableDescriptor).getStateDemand());
    }

}
