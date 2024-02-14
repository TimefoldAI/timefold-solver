package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;

public interface ListVariableElementStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object> {

    ElementState getState(Object element);

    /**
     * Counts all elements not in the {@link ElementState#ASSIGNED} state.
     *
     * @return >= 0.
     */
    int countNotAssigned();

    /**
     * State of an element of a list variable.
     */
    enum ElementState {
        /**
         * The element is initialized, but not yet assigned.
         * It is not part of any list.
         */
        INITIALIZED,
        /**
         * The element is assigned to a single list.
         */
        ASSIGNED
    }

}
