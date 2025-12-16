package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListUnassignMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Entity_ sourceEntity;
    private final int sourceIndex;

    private @Nullable Value_ unassignedValue;

    ListUnassignMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity,
            int sourceIndex) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sourceEntity = Objects.requireNonNull(sourceEntity);
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("The sourceIndex (" + sourceIndex + ") must be greater than or equal to 0.");
        }
        this.sourceIndex = sourceIndex;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        unassignedValue = solutionView.unassignValue(variableMetaModel, sourceEntity, sourceIndex);
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return new ListUnassignMove<>(variableMetaModel, rebaser.rebase(sourceEntity), sourceIndex);
    }

    @Override
    public Collection<Entity_> getPlanningEntities() {
        return Collections.singleton(sourceEntity);
    }

    @Override
    public Collection<Value_> getPlanningValues() {
        return Collections.singleton(getUnassignedValue());
    }

    private Value_ getUnassignedValue() {
        if (unassignedValue == null) {
            unassignedValue =
                    Objects.requireNonNull(getVariableDescriptor(variableMetaModel).getElement(sourceEntity, sourceIndex));
        }
        return unassignedValue;
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return Collections.singletonList(variableMetaModel);
    }

    public Entity_ getSourceEntity() {
        return sourceEntity;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ListUnassignMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(sourceEntity, other.sourceEntity)
                && sourceIndex == other.sourceIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, sourceEntity, sourceIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d] -> null}", getUnassignedValue(), sourceEntity, sourceIndex);
    }
}
