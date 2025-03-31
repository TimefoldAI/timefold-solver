package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

public final class ListUnassignMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Value_ movedValue;
    private final Entity_ sourceEntity;
    private final int sourceIndex;

    public ListUnassignMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value,
            Entity_ sourceEntity, int sourceIndex) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.movedValue = Objects.requireNonNull(value);
        this.sourceEntity = Objects.requireNonNull(sourceEntity);
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("The sourceIndex (" + sourceIndex + ") must be greater than or equal to 0.");
        }
        this.sourceIndex = sourceIndex;
    }

    @Override
    public void execute(@NonNull MutableSolutionView<Solution_> solutionView) {
        solutionView.unassignValue(variableMetaModel, movedValue, sourceEntity, sourceIndex);
    }

    @Override
    public @NonNull Move<Solution_> rebase(@NonNull Rebaser rebaser) {
        return new ListUnassignMove<>(variableMetaModel, rebaser.rebase(movedValue), rebaser.rebase(sourceEntity),
                sourceIndex);
    }

    @Override
    public @NonNull Collection<?> extractPlanningEntities() {
        return Collections.singleton(sourceEntity);
    }

    @Override
    public @NonNull Collection<?> extractPlanningValues() {
        return Collections.singleton(movedValue);
    }

    @Override
    protected List<VariableMetaModel<Solution_, ?, ?>> getVariableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ListUnassignMove<?, ?, ?> that))
            return false;
        return sourceIndex == that.sourceIndex && Objects.equals(variableMetaModel, that.variableMetaModel)
                && Objects.equals(sourceEntity, that.sourceEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, sourceEntity, sourceIndex);
    }

    @Override
    public @NonNull String toString() {
        return String.format("%s {%s[%d] -> null}", movedValue, sourceEntity, sourceIndex);
    }
}
