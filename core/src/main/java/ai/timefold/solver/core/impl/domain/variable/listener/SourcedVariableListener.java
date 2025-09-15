package ai.timefold.solver.core.impl.domain.variable.listener;

import ai.timefold.solver.core.impl.domain.variable.ChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;

/**
 * Used to externalize data for a {@link Supply} from the domain model itself.
 */
@NullMarked
public interface SourcedVariableListener<Solution_, ChangeEvent_ extends ChangeEvent> extends
        InnerVariableListener<Solution_, ChangeEvent_>, Supply {

    VariableDescriptor<Solution_> getSourceVariableDescriptor();

}
