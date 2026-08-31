package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;

/**
 * Draws {@link Sample}s, governed by a {@link Sampler}, out of the entities
 * whose given basic planning variable is currently unassigned (null),
 * and creates a move to assign every member to the same non-null destination value,
 * one that is legal for every member.
 * Members need not share anything beyond currently being unassigned;
 * unlike the pillar family, this draws with no grouping key.
 * <p>
 * {@code MassChangeMoveProvider} makes this same kind of move too,
 * whenever its own {@code crossingNull} is {@code true},
 * but only as a side effect of a mixed-value sample happening to include an unassigned member,
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 * <p>
 * Samples of size less than 2 are excluded: {@code AssignMoveProvider} already covers them, more cheaply.
 * A {@link Sampler} whose very first {@code evaluate(0, ...)} call already returns {@code STOP}
 * or {@code ACCEPT_AND_STOP} produces only size-1 samples,
 * which this provider discards outright;
 * use {@code MassUnassignMoveProvider},
 * or a sampler whose {@link Sampler#minimumSize() minimumSize} is at least 2 -
 * {@link Samplers#between(int, int) Samplers.between(2, n)} is the recommended choice,
 * since an unbounded {@link Samplers#all() Samplers.all()} makes this provider's move cost linear in the data set size.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 * @see MassChangeMoveProvider Mixed-value sample that may include already-assigned entities.
 * @see MassUnassignMoveProvider Unassigning an already-assigned sample.
 * @see AssignMoveProvider Assigning a single entity at a time.
 */
@NullMarked
public final class MassAssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Entity_> sampler;

    public MassAssignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Entity_> sampler) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
        this.sampler = Objects.requireNonNull(sampler);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var unassignedEntityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false)
                .filter((solutionView, entity) -> solutionView.getValue(variableMetaModel, entity) == null)
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new MassDestinationMoveIterator<>(session, random,
                variableMetaModel, unassignedEntityDataset, sampler, false));
    }

}
