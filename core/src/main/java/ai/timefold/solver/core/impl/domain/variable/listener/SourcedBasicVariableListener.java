package ai.timefold.solver.core.impl.domain.variable.listener;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableListener;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;

/**
 * Used to externalize data for a {@link Supply} from the domain model itself.
 */
@NullMarked
public non-sealed interface SourcedBasicVariableListener<Solution_, Entity_> extends
        SourcedVariableListener<Solution_, BasicVariableChangeEvent<Entity_>>, BasicVariableListener<Solution_, Entity_>,
        Supply {

}
