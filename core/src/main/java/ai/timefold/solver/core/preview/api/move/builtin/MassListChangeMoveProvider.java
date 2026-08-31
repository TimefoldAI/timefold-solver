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
 * out of the values currently assigned to any entity's {@link PlanningListVariable list variable},
 * and creates a move to gather every member consecutively,
 * in sample iteration order,
 * at one destination position legal for every member.
 * Members need not share an entity or be adjacent;
 * unlike {@code SubListChangeMoveProvider}, this draws a scattered sample, not a contiguous span.
 * <p>
 * This provider never assigns: its source is currently-assigned values only.
 * Samples of size less than 2 are excluded: {@code ListChangeMoveProvider} already covers them, more cheaply.
 * A {@link Sampler} whose very first {@code evaluate(0, ...)} call already returns {@code STOP}
 * or {@code ACCEPT_AND_STOP} produces only size-1 samples,
 * which this provider discards outright;
 * a sampler whose {@link Sampler#minimumSize() minimumSize} is at least 2 avoids that -
 * {@link Samplers#between(int, int) Samplers.between(2, n)} is the recommended choice,
 * since an unbounded {@link Samplers#all() Samplers.all()} makes this provider's move cost linear in the data set size.
 * <p>
 * A sample already sitting consecutively at the destination produces a move that changes nothing;
 * this is accepted, in the same spirit as a mixed-value {@code MassChangeMoveProvider} sample
 * that may leave some members unchanged.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 * @see SubListChangeMoveProvider A contiguous span drawn instead of a scattered sample.
 * @see MassListUnassignMoveProvider Unassigning a sample at a much higher rate.
 * @see MassListAssignMoveProvider A sampled set of currently-unassigned values.
 * @see ListChangeMoveProvider Moving a single already-assigned value at a time.
 */
@NullMarked
public class MassListChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Value_> sampler;
    private final boolean crossingNull;

    public MassListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Value_> sampler) {
        this(variableMetaModel, sampler, variableMetaModel.allowsUnassignedValues());
    }

    /**
     * @param crossingNull if {@code true}, also creates a move that unassigns the whole drawn sample;
     *        requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned
     *        values},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public MassListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Value_> sampler, boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sampler = Objects.requireNonNull(sampler);
        if (crossingNull && !variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException("""
                    The crossingNull (true) of variableMetaModel (%s) requires a variable \
                    which allows unassigned values, but this variable does not.
                    Maybe set crossingNull to false."""
                    .formatted(variableMetaModel));
        }
        this.crossingNull = crossingNull;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var sourceDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        // Only widen to forEachDestinationIncludingUnassigned when crossingNull:
        // unlike forEachDestination, it represents the unassigned destination with a null entity internally,
        // which entity-provided value ranges cannot resolve -
        // avoid tripping that path when this provider has no use for it anyway.
        var destinationDataset = (crossingNull
                ? moveStreamFactory.forEachDestinationIncludingUnassigned(variableMetaModel)
                : moveStreamFactory.forEachDestination(variableMetaModel)
                        .map((solutionView, position) -> (ElementPosition) position))
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new MassListDestinationMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, destinationDataset, sampler));
    }

}
