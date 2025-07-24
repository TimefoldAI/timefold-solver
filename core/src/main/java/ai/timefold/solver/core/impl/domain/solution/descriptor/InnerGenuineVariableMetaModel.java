package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface InnerGenuineVariableMetaModel<Solution_>
        extends InnerVariableMetaModel<Solution_>
        permits DefaultPlanningVariableMetaModel, DefaultPlanningListVariableMetaModel {

    @Override
    GenuineVariableDescriptor<Solution_> variableDescriptor();

}