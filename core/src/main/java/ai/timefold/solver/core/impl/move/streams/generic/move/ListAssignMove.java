package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

public final class ListAssignMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Value_ planningValue;
    private final Entity_ destinationEntity;
    private final int destinationIndex;

    public ListAssignMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
            Entity_ destinationEntity, int destinationIndex) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.planningValue = Objects.requireNonNull(planningValue);
        this.destinationEntity = Objects.requireNonNull(destinationEntity);
        if (destinationIndex < 0) {
            throw new IllegalArgumentException("The destinationIndex (" + destinationIndex + ") must be greater than 0.");
        }
        this.destinationIndex = destinationIndex;
    }

    @Override
    public void execute(@NonNull MutableSolutionView<Solution_> mutableSolutionView) {
        mutableSolutionView.assignValue(variableMetaModel, planningValue, destinationEntity, destinationIndex);
    }

    @Override
    public @NonNull Move<Solution_> rebase(@NonNull Rebaser rebaser) {
        return new ListAssignMove<>(variableMetaModel, rebaser.rebase(planningValue),
                rebaser.rebase(destinationEntity), destinationIndex);
    }

    @Override
    public @NonNull Collection<?> extractPlanningEntities() {
        return List.of(destinationEntity);
    }

    @Override
    public @NonNull Collection<?> extractPlanningValues() {
        return List.of(planningValue);
    }

    @Override
    protected List<VariableMetaModel<Solution_, ?, ?>> getVariableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public @NonNull String toString() {
        return String.format("%s {null -> %s[%d]}", planningValue, destinationEntity, destinationIndex);
    }
}
