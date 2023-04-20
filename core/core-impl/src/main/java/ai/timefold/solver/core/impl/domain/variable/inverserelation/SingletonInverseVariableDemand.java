package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class SingletonInverseVariableDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, SingletonInverseVariableSupply> {

    public SingletonInverseVariableDemand(VariableDescriptor<Solution_> sourceVariableDescriptor) {
        super(sourceVariableDescriptor);
    }

    // ************************************************************************
    // Creation method
    // ************************************************************************

    @Override
    public SingletonInverseVariableSupply createExternalizedSupply(SupplyManager supplyManager) {
        return new ExternalizedSingletonInverseVariableSupply<>(variableDescriptor);
    }

}
