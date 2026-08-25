package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.AbstractVariableDescriptorBasedDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * To get an instance, demand a {@link BasicVariableStateDemand} from {@link InnerScoreDirector#getSupplyManager()}.
 */
public final class BasicVariableStateDemand<Solution_>
        extends AbstractVariableDescriptorBasedDemand<Solution_, BasicVariableStateSupply<Solution_>> {

    public BasicVariableStateDemand(VariableDescriptor<Solution_> variableDescriptor) {
        super(variableDescriptor);
    }

    @Override
    public BasicVariableStateSupply<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
        return new ExternalizedBasicVariableStateSupply<>(variableDescriptor, supplyManager.getStateChangeNotifier());
    }

}
