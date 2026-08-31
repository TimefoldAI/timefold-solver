package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.move.AbstractMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Gathers every member of a {@link Sample} of a {@link PlanningListVariable list variable} - wherever each one currently is,
 * assigned or not
 * and inserts them consecutively, in sample iteration order, at one destination position.
 * A {@code null} destination unassigns every member instead of inserting them anywhere.
 * <p>
 * This is the list-variable equivalent of {@code MassChangeMove}:
 * an assign is a move whose members currently hold no position,
 * and an unassign is a move whose destination is {@code null},
 * so neither needs a class of its own.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningListVariable} annotation
 */
@NullMarked
public final class MassListChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sample<Value_> sample;
    private final @Nullable PositionInList destination;

    MassListChangeMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Sample<Value_> sample,
            @Nullable PositionInList destination) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sample = Objects.requireNonNull(sample);
        this.destination = destination;
    }

    public Sample<Value_> getSample() {
        return sample;
    }

    public @Nullable PositionInList getDestination() {
        return destination;
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        solutionView.massMoveValues(variableMetaModel, sample, destination);
    }

    @Override
    public MassListChangeMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        var rebasedDestination = destination == null ? null : destination.rebase(lookup);
        return new MassListChangeMove<>(variableMetaModel, sample.rebase(lookup), rebasedDestination);
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // This misses the source entities;
        // we accept that, as collecting them would have been far too expensive,
        // and only happens during execute() -
        // therefore the method would not provide the same result during the entire lifetime of the move.
        return destination == null ? List.of() : List.of(destination.<Entity_> entity());
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        var valueList = new ArrayList<>(sample.size());
        for (var member : sample) {
            valueList.add(Objects.requireNonNull(member));
        }
        return valueList;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MassListChangeMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(destination, other.destination)
                && sampleEquals(other);
    }

    // Insertion is order-sensitive (execute() gathers members in sample iteration order),
    // so equality must be too: Move requires that equal moves produce the exact same solution.
    // A null destination unassigns every member instead, where order does not affect the
    // resulting solution, so Sample's own order-insensitive equality is correct there.
    private boolean sampleEquals(MassListChangeMove<?, ?, ?> other) {
        if (destination == null) {
            return Objects.equals(sample, other.sample);
        }
        if (sample.size() != other.sample.size()) {
            return false;
        }
        var iterator = sample.iterator();
        var otherIterator = other.sample.iterator();
        while (iterator.hasNext()) {
            if (!Objects.equals(iterator.next(), otherIterator.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        var hash = 31 + Objects.hashCode(variableMetaModel);
        hash = hash * 31 + Objects.hashCode(destination);
        if (destination == null) {
            hash = hash * 31 + Objects.hashCode(sample);
        } else {
            for (var member : sample) {
                hash = hash * 31 + Objects.hashCode(member);
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        return sample + " -> " + destination;
    }

}
