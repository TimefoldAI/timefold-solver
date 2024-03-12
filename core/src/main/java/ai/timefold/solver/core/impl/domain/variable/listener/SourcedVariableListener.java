package ai.timefold.solver.core.impl.domain.variable.listener;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

/**
 * Used to externalize data for a {@link Supply} from the domain model itself.
 */
public interface SourcedVariableListener<Solution_> extends AbstractVariableListener<Solution_, Object>, Supply {

    VariableDescriptor<Solution_> getSourceVariableDescriptor();

}
