package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public sealed interface InnerVariableMetaModel<Solution_>
        permits DefaultBasicVariableMetaModel, DefaultListVariableMetaModel, DefaultShadowVariableMetaModel {

    VariableDescriptor<Solution_> variableDescriptor();

}
