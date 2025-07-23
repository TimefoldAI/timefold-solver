package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface InnerVariableMetaModel<Solution_>
        permits DefaultShadowVariableMetaModel, InnerGenuineVariableMetaModel {

    VariableDescriptor<Solution_> variableDescriptor();

}
