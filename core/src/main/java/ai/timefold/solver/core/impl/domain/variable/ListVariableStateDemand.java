package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class ListVariableStateDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, ListVariableStateSupply<Solution_, Object, Object>> {

    public ListVariableStateDemand(ListVariableDescriptor<Solution_> variableDescriptor) {
        super(variableDescriptor);
    }

    @Override
    public ListVariableStateSupply<Solution_, Object, Object> createExternalizedSupply(SupplyManager supplyManager) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
        return new ExternalizedListVariableStateSupply<>(listVariableDescriptor, supplyManager.getStateChangeNotifier());
    }

}
