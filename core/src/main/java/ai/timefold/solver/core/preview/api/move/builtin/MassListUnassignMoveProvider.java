package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.util.MappingIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;

/**
 * Draws {@link Sample}s, governed by a {@link Sampler} -
 * see {@link Samplers} for ready-made ones,
 * since an unbounded sampler makes this provider's move cost linear in the data set size -
 * out of the values currently assigned to any entity's {@link PlanningListVariable list variable},
 * and creates a move to unassign every member at once.
 * Members need not share an entity or be adjacent;
 * unlike {@code SubListUnassignMoveProvider}, this draws a scattered sample, not a contiguous span.
 * <p>
 * Requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned values};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 * <p>
 * {@code MassListChangeMoveProvider} makes this same kind of move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only as one destination row among many, so it arrives rarely.
 * This class exists to make it happen often.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 * @see SubListUnassignMoveProvider A contiguous span drawn instead of a scattered sample.
 * @see MassListChangeMoveProvider Unassigning a whole drawn sample as one destination row among many.
 * @see ListUnassignMoveProvider Unassigning a single value at a time.
 * @see MassListAssignMoveProvider Assigning unassigned values.
 */
@NullMarked
public final class MassListUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Value_> sampler;

    public MassListUnassignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Value_> sampler) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
        this.sampler = Objects.requireNonNull(sampler);
    }

    /**
     * Draws mixed samples of currently-assigned values and unassigns every member,
     * producing a {@code MassListChangeMove} with a null destination.
     * The destination is fixed at null, so nothing can ever be rejected:
     * every drawn sample yields a valid move, with no probing.
     * <p>
     * Unlike {@link MassListAssignMoveProvider}/{@link MassListChangeMoveProvider},
     * size-1 samples are <strong>not</strong> skipped here, and deliberately so:
     * unassigning one value is no more expensive than unassigning several,
     * so there is nothing to gain by discarding it.
     * A {@link Sampler}
     * whose {@link Sampler#minimumSize() minimumSize} is greater than 1 can still refuse a draw on a dataset smaller than that
     * minimum -
     * {@code samplingIterator}'s {@code hasNext()} then simply returns {@code false} for that call,
     * ending this iterator without spinning,
     * since each call is independent and tries a fresh source.
     */
    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var assignedValueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new MappingIterator<>(
                session.getInstance(assignedValueDataset).samplingIterator(sampler, random),
                sample -> Moves.massChange(variableMetaModel, sample, null)));
    }

}
