package ai.timefold.solver.core.api.move.generic;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.MutableSolutionState;
import ai.timefold.solver.core.api.move.factory.ContextlessMove;
import ai.timefold.solver.core.api.move.factory.Rebaser;

public record ChangeMove<Solution_, Entity_, Value_>(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
        Entity_ entity, Value_ value)
        implements
            ContextlessMove<Solution_> {

    @Override
    public void run(MutableSolutionState<Solution_> mutableSolutionState) {
        mutableSolutionState.changeVariable(variableMetaModel, entity, value);
    }

    @Override
    public ContextlessMove<Solution_> rebase(Rebaser rebaser) {
        return new ChangeMove<>(variableMetaModel, rebaser.apply(entity), rebaser.apply(value));
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
