package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;

public interface ListVariableStateSupply<Solution_> extends
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        SingletonInverseVariableSupply,
        IndexVariableSupply,
        ListVariableElementStateSupply<Solution_> {

    void externalizeSingletonListInverseVariable(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void enablePreviousElementShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void enableNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    @Override
    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();
}
