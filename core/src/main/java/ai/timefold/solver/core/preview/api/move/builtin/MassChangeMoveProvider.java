package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;

/**
 * Draws {@link Sample}s, governed by a {@link Sampler},
 * out of the entities of the given basic planning variable's declaring class,
 * and creates a move to change every member's value to a different value that is legal for every member.
 * Members need not share a value.
 * A sample that happens to hold a single shared value is legal too;
 * its own value is never offered as a destination.
 * <p>
 * When {@code crossingNull} is {@code true}
 * (the default whenever the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned values}),
 * This provider's source also admits unassigned entities,
 * so a drawn sample may contain them and get them assigned as a side effect -
 * a side effect, not a directed draw, so its rate follows the fraction of entities currently unassigned.
 * The same flag also lets a drawn sample be unassigned as a whole -
 * probability {@code 1/(s+1)}, where {@code s} is the size of the sample members' value range.
 * When {@code false}, the source excludes unassigned entities and no unassign move is produced either:
 * for more assign/unassign moves at a much higher rate,
 * use {@code MassAssignMoveProvider}/{@code MassUnassignMoveProvider}.
 * <p>
 * A mixed-value sample can produce a move that leaves some members unchanged;
 * this is intentional.
 * {@link MoveProviderUtil#sharedValueOf} excludes a destination only when
 * every member of the sample already agrees on it;
 * for a mixed sample it returns {@code null}, and {@code null} excludes nothing,
 * so the destination may land on a value some (but not all) members already hold.
 * Excluding per member instead is rejected on purpose:
 * it would also block a legitimate move that collects a scattered sample onto a value one member holds already,
 * it would need as many exclusions as there are members instead of one,
 * and it would break the destination iterator's fast path.
 * Each unchanged member still pays a full variable-change notification and shadow-variable recalculation,
 * in the move and in its undo;
 * that cost is accepted as the price of a single move over an otherwise arbitrary sample.
 * <p>
 * Samples of size less than 2 are excluded:
 * {@code ChangeMoveProvider}/{@code AssignMoveProvider} already cover them, more cheaply.
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
 * @see MassAssignMoveProvider Every member is currently unassigned, with no mixed-value side effect.
 * @see MassUnassignMoveProvider Unassigning a mixed-value sample at a much higher rate.
 * @see ChangeMoveProvider Changing a single entity at a time.
 * @see PillarChangeMoveProvider Changing the whole pillar of every entity sharing a value at once.
 * @see SubPillarChangeMoveProvider A sampler-driven subset of such a pillar.
 * @see AssignMoveProvider Assigning a single entity at a time.
 */
@NullMarked
public final class MassChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Sampler<Entity_> sampler;
    private final boolean crossingNull;

    public MassChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Entity_> sampler) {
        this(variableMetaModel, sampler, variableMetaModel.allowsUnassigned());
    }

    /**
     * @param crossingNull if {@code true}, the source admits unassigned entities
     *        (so a drawn sample may get them assigned)
     *        and a drawn sample may be unassigned as a whole;
     *        requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public MassChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sampler<Entity_> sampler, boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sampler = Objects.requireNonNull(sampler);
        if (crossingNull && !variableMetaModel.allowsUnassigned()) {
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
        var sourceDataset = crossingNull
                ? MoveProviderUtil.allEntities(moveStreamFactory, variableMetaModel)
                : MoveProviderUtil.assignedEntityDataset(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new MassDestinationMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, sampler, crossingNull));
    }

}
