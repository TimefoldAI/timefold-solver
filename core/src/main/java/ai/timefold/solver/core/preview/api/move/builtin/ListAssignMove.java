package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ListAssignMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Value_ planningValue;
    private final Entity_ destinationEntity;
    private final int destinationIndex;

    ListAssignMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
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
    public void execute(MutableSolutionView<Solution_> mutableSolutionView) {
        mutableSolutionView.assignValue(variableMetaModel, planningValue, destinationEntity, destinationIndex);
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return new ListAssignMove<>(variableMetaModel, Objects.requireNonNull(rebaser.rebase(planningValue)),
                Objects.requireNonNull(rebaser.rebase(destinationEntity)), destinationIndex);
    }

    @Override
    public Collection<Entity_> getPlanningEntities() {
        return List.of(destinationEntity);
    }

    @Override
    public Collection<Value_> getPlanningValues() {
        return List.of(planningValue);
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    public Value_ getPlanningValue() {
        return planningValue;
    }

    public Entity_ getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ListAssignMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(planningValue, other.planningValue)
                && Objects.equals(destinationEntity, other.destinationEntity)
                && destinationIndex == other.destinationIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, planningValue, destinationEntity, destinationIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {null -> %s[%d]}", planningValue, destinationEntity, destinationIndex);
    }
}
