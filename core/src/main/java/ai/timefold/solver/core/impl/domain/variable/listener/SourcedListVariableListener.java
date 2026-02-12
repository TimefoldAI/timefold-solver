package ai.timefold.solver.core.impl.domain.variable.listener;

import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;

/**
 * Used to externalize data for a {@link Supply} from the domain model itself.
 */
@NullMarked
public non-sealed interface SourcedListVariableListener<Solution_, Entity_, Element_> extends
        SourcedVariableListener<Solution_, ListElementsChangeEvent<Entity_>>,
        ListVariableListener<Solution_, Entity_, Element_>, Supply {

}
