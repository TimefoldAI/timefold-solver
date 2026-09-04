package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;

/**
 * Draws {@link Sample}s, governed by a {@link Sampler},
 * out of the values currently unassigned to any entity's {@link PlanningListVariable list variable},
 * and creates a move to insert every member consecutively, in sample iteration order,
 * at one destination position legal for every member.
 * Members need not share anything beyond currently being unassigned.
 * <p>
 * Requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned values};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 * <p>
 * Samples of size less than 2 are excluded: {@code ListAssignMoveProvider} already covers them, more cheaply.
 * A {@link Sampler} whose very first {@code evaluate(0, ...)} call already returns {@code STOP}
 * or {@code ACCEPT_AND_STOP} produces only size-1 samples,
 * which this provider discards outright;
 * a sampler whose {@link Sampler#minimumSize() minimumSize} is at least 2 avoids that -
 * {@link Samplers#between(int, int) Samplers.between(2, n)} is the recommended choice,
 * since an unbounded {@link Samplers#all() Samplers.all()} makes this provider's move cost linear in the data set size.
 * <p>
 * {@code MassListChangeMoveProvider} never produces this kind of move at all,
 * since its own source is currently assigned values only.
 * This class exists to make it happen.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 * @see MassListChangeMoveProvider Mixed-value sample that may include already-assigned values.
 * @see MassListUnassignMoveProvider Unassigning an already-assigned sample.
 * @see ListAssignMoveProvider Assigning a single value at a time.
 */
@NullMarked
public final class MassListAssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Value_> sampler;

    public MassListAssignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Value_> sampler) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
        this.sampler = Objects.requireNonNull(sampler);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var sourceDataset = moveStreamFactory.forEachUnassignedValue(variableMetaModel).asCachedDataset();
        var destinationDataset = moveStreamFactory.forEachDestination(variableMetaModel)
                .map((solutionView, position) -> (ElementPosition) position)
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new MassListDestinationMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, destinationDataset, sampler));
    }

}
