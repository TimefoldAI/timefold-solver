package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;

public interface ListVariableDataSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        SingletonInverseVariableSupply,
        IndexVariableSupply,
        ListVariableElementStateSupply<Solution_> {

    /**
     * Returns the state that the element of the list variable is in.
     * To avoid hash lookups, calling {@link #countNotAssigned()} is possible;
     * if it returns 0, the element is definitely {@link ElementState#ASSIGNED}.
     *
     * @param element never null
     * @return never null
     */
    @Override
    ElementState getState(Object element);

    /**
     *
     * @return number of items for which {@link #getState(Object)} would return anything but {@link ElementState#ASSIGNED}.
     */
    @Override
    int countNotAssigned();

    /**
     *
     * @param planningValue never null
     * @return number >= 0 if planningValue is {@link ElementState#ASSIGNED},
     *         otherwise null.
     */
    @Override
    Integer getIndex(Object planningValue);

    /**
     *
     * @param planningValue never null
     * @return entity whose list variable contains the value if planningValue is {@link ElementState#ASSIGNED},
     *         otherwise null.
     */
    @Override
    Object getInverseSingleton(Object planningValue);

    /**
     *
     * @param value never null
     * @return never null
     */
    ElementLocation getLocationInList(Object value);

    @Override
    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();
}
