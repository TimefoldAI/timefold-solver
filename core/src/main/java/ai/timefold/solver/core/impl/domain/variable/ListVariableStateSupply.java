package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;

/**
 * Single source of truth for all information about elements inside list variables.
 * Shadow variables can be connected to this class to save on iteration costs
 * that would've been incurred otherwise if using variable listeners for each of them independently.
 * This way, there is only one variable listener for all such shadow variables,
 * and therefore only a single iteration to update all the information.
 * 
 * @param <Solution_>
 */
public interface ListVariableStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        SingletonInverseVariableSupply,
        IndexVariableSupply {

    void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalizeSingletonListInverseVariable(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalizePreviousElementShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalizeNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    @Override
    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();

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
     * Consider calling this before {@link #isAssigned(Object)} to eliminate some map accesses.
     * If unassigned count is 0, {@link #isAssigned(Object)} is guaranteed to return true.
     *
     * @return number of elements for which {@link #isAssigned(Object)} would return false.
     */
    int getUnassignedCount();

    /**
     *
     * @param element never null
     * @return null if the element is the first element in the list
     */
    Object getPreviousElement(Object element);

    /**
     *
     * @param element never null
     * @return null if the element is the last element in the list
     */
    Object getNextElement(Object element);

}
