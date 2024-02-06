package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class ListVariableDataDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, ListVariableDataSupply<Solution_>> {

    public ListVariableDataDemand(ListVariableDescriptor<Solution_> variableDescriptor) {
        super(variableDescriptor);
    }

    @Override
    public ListVariableDataSupply<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
        return new ExternalizedListVariableDataSupply<>(listVariableDescriptor);
    }

}
