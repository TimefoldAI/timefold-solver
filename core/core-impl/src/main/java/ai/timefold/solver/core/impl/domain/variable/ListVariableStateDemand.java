package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class ListVariableStateDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, ListVariableStateSupply<Solution_>> {

    public ListVariableStateDemand(ListVariableDescriptor<Solution_> variableDescriptor) {
        super(variableDescriptor);
    }

    @Override
    public ListVariableStateSupply<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
        return new ExternalizedListVariableStateSupply<>(listVariableDescriptor);
    }

}
