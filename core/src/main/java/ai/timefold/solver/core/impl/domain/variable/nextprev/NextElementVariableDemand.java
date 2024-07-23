package ai.timefold.solver.core.impl.domain.variable.nextprev;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class NextElementVariableDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, ExternalizedNextElementVariableSupply<Solution_>> {

    public NextElementVariableDemand(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        super(sourceVariableDescriptor);
    }

    // ************************************************************************
    // Creation method
    // ************************************************************************

    @Override
    public ExternalizedNextElementVariableSupply<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
        return new ExternalizedNextElementVariableSupply<>((ListVariableDescriptor<Solution_>) variableDescriptor);
    }
}
