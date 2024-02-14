package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;

public interface ListVariableElementStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object> {

    /**
     *
     * @param element never null
     * @return true if the element is contained in a list variable of any entity.
     */
    boolean isAssigned(Object element);

    /**
     *
     * @param value never null
     * @return never null
     */
    ElementLocation getLocationInList(Object value);

    /**
     *
     * @return number of elements for which {@link #isAssigned(Object)} would return false.
     */
    int countUnassigned();

}
