package ai.timefold.solver.core.impl.move.builtin;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.move.AbstractMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unassigns a contiguous span of a {@link PlanningListVariable list variable},
 * that is, removes every value of the span from the list, leaving it unassigned.
 * The span is identified by a {@link Range}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public final class SubListUnassignMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Range<Entity_> range;

    private @Nullable List<Value_> unassignedValues;

    public SubListUnassignMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Range<Entity_> range) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.range = Objects.requireNonNull(range);
    }

    public Range<Entity_> getRange() {
        return range;
    }

    @SuppressWarnings("unchecked")
    private List<Value_> getUnassignedValues() {
        if (unassignedValues == null) {
            var list = (List<Value_>) getVariableDescriptor(variableMetaModel).getValue(range.entity());
            unassignedValues = List.copyOf(list.subList(range.fromIndex(), range.toIndex()));
        }
        return unassignedValues;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        unassignedValues =
                solutionView.unassignValues(variableMetaModel, range.entity(), range.fromIndex(), range.toIndex());
    }

    @Override
    public SubListUnassignMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        return new SubListUnassignMove<>(variableMetaModel, range.rebase(lookup));
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(range.entity());
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.copyOf(getUnassignedValues());
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubListUnassignMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(range, other.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, range);
    }

    @Override
    public String toString() {
        return "|%d| {%s -> null}".formatted(range.length(), range);
    }

}
