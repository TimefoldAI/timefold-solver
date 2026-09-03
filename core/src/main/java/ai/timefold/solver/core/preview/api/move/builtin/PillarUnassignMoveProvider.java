package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.util.MappingIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;

import org.jspecify.annotations.NullMarked;

/**
 * Draws pillars of entities sharing a non-null value ("slice value") of the given variable
 * and creates a move to unassign every member at once
 * (set the basic planning variable to null).
 * The pillar is keyed on this one variable alone; members may differ in every other variable.
 * <p>
 * {@code PillarChangeMoveProvider} makes this same move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only with probability {@code 1/(s+1)} per drawn pillar
 * (where {@code s} is the size of the pillar members' value range),
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * Draws whole pillars only, unbounded by design:
 * a pillar move is defined as moving every member of the pillar,
 * so its cost is linear in the pillar's size with no cap.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @see PillarChangeMoveProvider Changing the whole pillar too, as one candidate among many.
 * @see SubPillarUnassignMoveProvider A sampler-driven, size-bounded subset of the pillar.
 * @see UnassignMoveProvider Unassigning a single entity at a time.
 * @see MassAssignMoveProvider Assigning unassigned entities.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class PillarUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public PillarUnassignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    /**
     * Draws whole pillars (one cached row per assigned value) and unassigns every member,
     * producing a {@code MassChangeMove} with a null destination.
     * The destination is fixed at null, so nothing can ever be rejected:
     * every drawn pillar yields a valid move, with no probing and no left value to retire.
     * Plain sampling-with-replacement: nothing needs retiring, since a destination of null can never be rejected.
     */
    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var pillarDataset = assignedPillars(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new MappingIterator<>(
                session.getInstance(pillarDataset).iterator(random),
                pillar -> Moves.massChange(variableMetaModel, pillar, null)));
    }

    /**
     * One cached row per distinct assigned value,
     * each row a whole {@link Sample} of that value's members.
     * Built once per settle per changed group,
     * never re-assembled per draw - the drawing move providers read a row directly instead of running {@code SampleAssembler}
     * over an index every time.
     */
    private static <Solution_, Entity_, Value_> UniDataset<Solution_, Sample<Entity_>> assignedPillars(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return MoveProviderUtil.assignedEntities(moveStreamFactory, variableMetaModel)
                .groupBy((solutionView, entity) -> solutionView.getValue(variableMetaModel, entity),
                        NeighborhoodsCollectors.collectAndThen(
                                NeighborhoodsCollectors.<Solution_, Entity_> toList(), Sample::of))
                .map((solutionView, value, pillar) -> pillar)
                .asCachedDataset();
    }

}
