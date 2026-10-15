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
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Moves a contiguous span of a {@link PlanningListVariable list variable} to a different position,
 * possibly on a different entity.
 * The moved span is identified by a {@link Range}.
 * The span is inserted starting at the given destination position, optionally in reverse element order.
 * When the destination is on the same entity as the source, the destination index is interpreted
 * as if the span had already been removed - see {@link MutableSolutionView#moveValuesInList} for the exact contract.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public final class SubListChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Range<Entity_> source;
    private final PositionInList destination;
    private final boolean reversing;

    private @Nullable List<Value_> movedValues;

    public SubListChangeMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Range<Entity_> source,
            PositionInList destination, boolean reversing) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.reversing = reversing;
    }

    public Range<Entity_> getSource() {
        return source;
    }

    public PositionInList getDestination() {
        return destination;
    }

    public boolean isReversing() {
        return reversing;
    }

    @SuppressWarnings("unchecked")
    private List<Value_> getMovedValues() {
        if (movedValues == null) {
            var sourceList = (List<Value_>) getVariableDescriptor(variableMetaModel).getValue(source.entity());
            movedValues = List.copyOf(sourceList.subList(source.fromIndex(), source.toIndex()));
        }
        return movedValues;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        Entity_ sourceEntity = source.entity();
        Entity_ destinationEntity = destination.entity();
        movedValues = (sourceEntity == destinationEntity)
                ? solutionView.moveValuesInList(variableMetaModel, sourceEntity, source.fromIndex(), source.toIndex(),
                        destination.index(), reversing)
                : solutionView.moveValuesBetweenLists(variableMetaModel, sourceEntity, source.fromIndex(), source.toIndex(),
                        destinationEntity, destination.index(), reversing);
    }

    @Override
    public SubListChangeMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        return new SubListChangeMove<>(variableMetaModel, source.rebase(lookup), destination.rebase(lookup), reversing);
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        Entity_ sourceEntity = source.entity();
        Entity_ destinationEntity = destination.entity();
        if (sourceEntity == destinationEntity) {
            return List.of(sourceEntity);
        } else {
            return List.of(sourceEntity, destinationEntity);
        }
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.copyOf(getMovedValues());
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubListChangeMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(source, other.source)
                && Objects.equals(destination, other.destination)
                && reversing == other.reversing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, source, destination, reversing);
    }

    @Override
    public String toString() {
        return "|%d| {%s[%d..%d] -%s> %s[%d]}".formatted(
                source.length(), source.entity(), source.fromIndex(), source.toIndex() - 1,
                reversing ? "reversing-" : "", destination.<Entity_> entity(), destination.index());
    }

}
