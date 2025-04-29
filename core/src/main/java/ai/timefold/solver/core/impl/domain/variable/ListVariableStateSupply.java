package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

/**
 * Single source of truth for all information about elements inside {@link PlanningListVariable list variables}.
 * Shadow variables can be connected to this class to save on iteration costs
 * that would've been incurred otherwise if using variable listeners for each of them independently.
 * This way, there is only one variable listener for all such shadow variables,
 * and therefore only a single iteration to update all the information.
 *
 * <p>
 * If a particular shadow variable is externalized,
 * it means that there is a field on an entity holding the value of the shadow variable.
 * In this case, we will attempt to use that value.
 * Otherwise, we will keep an internal track of all the possible shadow variables
 * ({@link ai.timefold.solver.core.api.domain.variable.IndexShadowVariable},
 * {@link ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable},
 * {@link ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable},
 * {@link ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable}),
 * and use values from this internal representation.
 * 
 * @param <Solution_>
 * @see ListVariableState The logic of switching between internal and externalized shadow variables.
 * @see ExternalizedListVariableStateSupply The external representation of these shadow variables,
 *      which doesn't care whether the variable is internal or externalized.
 */
public interface ListVariableStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        SingletonInverseVariableSupply,
        IndexVariableSupply {

    void externalize(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

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
     * @param element never null
     * @return true if the element is in a pinned part of a list variable of any entity
     */
    boolean isPinned(Object element);

    /**
     *
     * @param value never null
     * @return never null
     */
    ElementPosition getElementPosition(Object value);

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
