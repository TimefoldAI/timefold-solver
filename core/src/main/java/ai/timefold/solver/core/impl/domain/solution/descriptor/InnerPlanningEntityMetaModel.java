package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
sealed interface InnerPlanningEntityMetaModel<Solution_, Entity_>
        permits DefaultGenuineEntityMetaModel, DefaultShadowEntityMetaModel {

    void addVariable(VariableMetaModel<Solution_, Entity_, ?> variable);

}