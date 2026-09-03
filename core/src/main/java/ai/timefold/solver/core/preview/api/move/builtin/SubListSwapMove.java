package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.move.AbstractMove;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Swaps two contiguous, non-overlapping spans of a {@link PlanningListVariable list variable},
 * possibly on different entities.
 * Each span is identified by a {@link Range}.
 * Left and right entity can be the same instance,
 * in which case the two spans must not overlap.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public final class SubListSwapMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Range<Entity_> leftRange;
    private final Range<Entity_> rightRange;
    private final boolean reversing;

    private @Nullable List<Value_> leftValues;
    private @Nullable List<Value_> rightValues;

    SubListSwapMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> leftRange,
            Range<Entity_> rightRange, boolean reversing) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        // Normalize so that, when both spans are on the same entity, left always precedes right;
        // this is what makes the spans satisfy MutableSolutionView.swapValuesInList's ordering precondition.
        if (leftRange.entity() == rightRange.entity() && leftRange.fromIndex() > rightRange.fromIndex()) {
            this.leftRange = rightRange;
            this.rightRange = leftRange;
        } else {
            this.leftRange = leftRange;
            this.rightRange = rightRange;
        }
        this.reversing = reversing;
    }

    public Range<Entity_> getLeftRange() {
        return leftRange;
    }

    public Range<Entity_> getRightRange() {
        return rightRange;
    }

    public boolean isReversing() {
        return reversing;
    }

    @SuppressWarnings("unchecked")
    private List<Value_> getLeftValues() {
        if (leftValues == null) {
            var list = (List<Value_>) getVariableDescriptor(variableMetaModel).getValue(leftRange.entity());
            leftValues = List.copyOf(list.subList(leftRange.fromIndex(), leftRange.toIndex()));
        }
        return leftValues;
    }

    @SuppressWarnings("unchecked")
    private List<Value_> getRightValues() {
        if (rightValues == null) {
            var list = (List<Value_>) getVariableDescriptor(variableMetaModel).getValue(rightRange.entity());
            rightValues = List.copyOf(list.subList(rightRange.fromIndex(), rightRange.toIndex()));
        }
        return rightValues;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        // Cache the pre-move values now, before either side is mutated.
        getLeftValues();
        getRightValues();
        var leftEntity = leftRange.entity();
        var rightEntity = rightRange.entity();
        if (leftEntity == rightEntity) {
            solutionView.swapValuesInList(variableMetaModel, leftEntity, leftRange.fromIndex(), leftRange.toIndex(),
                    rightRange.fromIndex(), rightRange.toIndex(), reversing);
        } else {
            solutionView.swapValuesBetweenLists(variableMetaModel, leftEntity, leftRange.fromIndex(), leftRange.toIndex(),
                    rightEntity, rightRange.fromIndex(), rightRange.toIndex(), reversing);
        }
    }

    @Override
    public SubListSwapMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        return new SubListSwapMove<>(variableMetaModel, leftRange.rebase(lookup), rightRange.rebase(lookup), reversing);
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        var leftEntity = leftRange.entity();
        var rightEntity = rightRange.entity();
        if (leftEntity == rightEntity) {
            return List.of(leftEntity);
        }
        return List.of(leftEntity, rightEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return CollectionUtils.concat(getLeftValues(), getRightValues());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubListSwapMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(leftRange, other.leftRange)
                && Objects.equals(rightRange, other.rightRange)
                && reversing == other.reversing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, leftRange, rightRange, reversing);
    }

    @Override
    public String toString() {
        return "{%s} <%s> {%s}"
                .formatted(leftRange, (reversing ? "-reversing-" : "-"), rightRange);
    }

}
