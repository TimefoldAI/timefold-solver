package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

public final class ChainedChangeMove<Solution_, Entity_> extends ChangeMove<Solution_, Entity_, Entity_> {

    private final Entity_ oldTrailingEntity;
    private final Entity_ newTrailingEntity;

    @SuppressWarnings("unchecked")
    public ChainedChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Entity_> variableMetaModel, Entity_ entity,
            Entity_ toPlanningValue, SingletonInverseVariableSupply inverseVariableSupply) {
        super(variableMetaModel, entity, toPlanningValue);
        this.oldTrailingEntity = (Entity_) Objects.requireNonNull(inverseVariableSupply).getInverseSingleton(entity);
        this.newTrailingEntity =
                toPlanningValue == null ? null : (Entity_) inverseVariableSupply.getInverseSingleton(toPlanningValue);
    }

    ChainedChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Entity_> variableMetaModel, Entity_ entity,
            Entity_ toPlanningValue, Entity_ oldTrailingEntity, Entity_ newTrailingEntity) {
        super(variableMetaModel, entity, toPlanningValue);
        this.oldTrailingEntity = oldTrailingEntity;
        this.newTrailingEntity = newTrailingEntity;
    }

    @Override
    public void execute(@NonNull MutableSolutionView<Solution_> solutionView) {
        // Close the old chain
        if (oldTrailingEntity != null) {
            solutionView.changeVariable(variableMetaModel, oldTrailingEntity, readValue(solutionView));
        }
        // Change the entity
        solutionView.changeVariable(variableMetaModel, entity, toPlanningValue);
        // Reroute the new chain
        if (newTrailingEntity != null) {
            solutionView.changeVariable(variableMetaModel, newTrailingEntity, entity);
        }
    }

    @Override
    public @NonNull ChainedChangeMove<Solution_, Entity_> rebase(@NonNull Rebaser rebaser) {
        return new ChainedChangeMove<>(variableMetaModel,
                rebaser.rebase(entity),
                rebaser.rebase(toPlanningValue),
                rebaser.rebase(oldTrailingEntity),
                rebaser.rebase(newTrailingEntity));
    }

}
