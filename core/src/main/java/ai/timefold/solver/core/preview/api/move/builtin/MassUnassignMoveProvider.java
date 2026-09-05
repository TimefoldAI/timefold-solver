package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.util.MappingIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
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
 * out of the entities whose given basic planning variable is currently assigned a non-null value, of any value,
 * and creates a move to unassign every member at once
 * (set the basic planning variable to null).
 * Members need not share a value;
 * unlike the pillar family, this draws with no grouping key.
 * <p>
 * {@code MassChangeMoveProvider} makes this same kind of move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only with probability {@code 1/(s+1)} per drawn sample
 * (where {@code s} is the size of the sample members' value range),
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 * @see MassChangeMoveProvider Unassigning a sample too, as one candidate among many.
 * @see PillarUnassignMoveProvider Unassigning the whole pillar of every entity sharing a value at once.
 * @see SubPillarUnassignMoveProvider A sampler-driven subset of such a pillar.
 * @see UnassignMoveProvider Unassigning a single entity at a time.
 * @see MassAssignMoveProvider Assigning unassigned entities.
 */
@NullMarked
public final class MassUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Entity_> sampler;

    public MassUnassignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Entity_> sampler) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
        this.sampler = Objects.requireNonNull(sampler);
    }

    /**
     * Draws mixed-value samples of currently-assigned entities and unassigns every member,
     * producing a {@code MassChangeMove} with a null destination.
     * The destination is fixed at null, so nothing can ever be rejected:
     * every drawn sample yields a valid move, with no probing.
     * <p>
     * Unlike {@link MassAssignMoveProvider}/{@link MassChangeMoveProvider},
     * size-1 samples are <strong>not</strong> skipped here, and deliberately so:
     * unassigning one entity is no more expensive than unassigning several,
     * so there is nothing to gain by discarding it.
     * A {@link Sampler} whose {@link Sampler#minimumSize() minimumSize} is greater than 1
     * can still refuse a draw on a dataset smaller than that minimum -
     * {@code samplingIterator}'s {@code hasNext()} then simply returns {@code false} for that call,
     * ending this iterator without spinning,
     * since each call is independent and tries a fresh source.
     */
    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var assignedEntityDataset = MoveProviderUtil.assignedEntityDataset(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new MappingIterator<>(
                session.getInstance(assignedEntityDataset).samplingIterator(sampler, random),
                sample -> Moves.massChange(variableMetaModel, sample, null)));
    }

}
