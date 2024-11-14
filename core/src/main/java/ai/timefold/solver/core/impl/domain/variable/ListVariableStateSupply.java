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
        IndexVariableSupply,
        ListVariableElementStateSupply<Solution_> {

    void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalizeSingletonListInverseVariable(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void enablePreviousElementShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void enableNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    @Override
    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();

}
