package ai.timefold.solver.core.impl.move.generic;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.move.ContextlessMove;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;

public record ChangeMove<Solution_, Entity_, Value_>(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
        Entity_ entity, Value_ value)
        implements
            ContextlessMove<Solution_> {

    @Override
    public void run(MutableSolutionState<Solution_> mutableSolutionState) {
        mutableSolutionState.changeVariable(variableMetaModel, entity, value);
    }

    @Override
    public ContextlessMove<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return new ChangeMove<>(variableMetaModel, solutionState.rebase(entity), solutionState.rebase(value));
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return List.of(entity);
    }

    @Override
    public Collection<?> getPlanningValues() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return entity + " -> " + value;
    }

}
