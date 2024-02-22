package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;

public interface ListVariableStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        SingletonInverseVariableSupply,
        IndexVariableSupply,
        ListVariableElementStateSupply<Solution_> {

    @Override
    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();
}
