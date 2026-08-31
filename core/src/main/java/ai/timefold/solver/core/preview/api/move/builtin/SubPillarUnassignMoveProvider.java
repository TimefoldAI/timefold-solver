package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws subpillars, governed by a {@link PillarSampler} -
 * see {@link Samplers#pillar(Sampler) Samplers.pillar} to lift a size-only policy,
 * since an unbounded sampler makes this provider's move cost linear in the pillar's size -
 * of entities sharing a non-null value ("slice value") of the given variable
 * and creates a move to unassign every member at once (set the basic planning variable to null).
 * The (sub)pillar is keyed on this one variable alone;
 * members may differ in every other variable.
 * <p>
 * {@code SubPillarChangeMoveProvider} makes this same move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only with probability {@code 1/(s+1)} per drawn subpillar
 * (where {@code s} is the size of the subpillar members' value range),
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @see SubPillarChangeMoveProvider Changing the whole subpillar too, as one candidate among many.
 * @see UnassignMoveProvider Unassigning a single entity at a time.
 * @see PillarUnassignMoveProvider Unassigning the whole pillar of every entity sharing the slice value at once.
 * @see MassAssignMoveProvider Mass-assigning unassigned entities.
 * @see MassUnassignMoveProvider Mass-unassigning a mixed-value sample with no shared key.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubPillarUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final PillarSampler<Value_, Entity_> sampler;

    public SubPillarUnassignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            PillarSampler<Value_, Entity_> sampler) {
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
        var distinctValueDataset = MoveProviderUtil.distinctAssignedValues(moveStreamFactory, variableMetaModel);
        var pillarSourceDataset = MoveProviderUtil.entitiesByAssignedValue(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new SubPillarUnassignMoveIterator<>(session, random,
                variableMetaModel, distinctValueDataset, pillarSourceDataset, sampler));
    }

    /**
     * Draws subpillars sharing an assigned value ("slice value") and unassigns every member,
     * producing a {@code MassChangeMove} with a null destination.
     * The destination is fixed at null, so a drawn subpillar's own move never gets rejected -
     * but the subpillar draw itself can be,
     * if the sampler refuses or the slice is smaller than the sampler's minimum size.
     * A slice value that keeps failing that draw is retired by {@link RetiringBiWalk}
     * after {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} attempts,
     * the same as every other {@code Sub*} provider in this package -
     * one bad slice value must not end the whole iterator,
     * since {@link PillarSampler} is contractually forbidden from stopping below its own {@link PillarSampler#minimumSize()},
     * so a failed draw is proof about that one slice, not about the rest of the domain.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class SubPillarUnassignMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, Sample<Entity_>> {

        private final RetiringRandomIterator<Value_> sliceValueIterator;
        private final BiDatasetInstance<Value_, Entity_> pillarSourceInstance;
        private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final PillarSampler<Value_, Entity_> sampler;
        private final RandomGenerator random;

        private @Nullable Move<Solution_> nextMove = null;

        public SubPillarUnassignMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> distinctValueDataset,
                BiDataset<Solution_, Value_, Entity_> pillarSourceDataset,
                PillarSampler<Value_, Entity_> sampler) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.sampler = Objects.requireNonNull(sampler);
            this.random = Objects.requireNonNull(random);
            var distinctValueInstance =
                    (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(distinctValueDataset);
            this.sliceValueIterator = distinctValueInstance.retiringRandomIterator(random);
            this.pillarSourceInstance = session.getInstance(pillarSourceDataset);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(sliceValueIterator, this);
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var move = Objects.requireNonNull(nextMove);
            nextMove = null;
            return move;
        }

        @Override
        public Iterator<Sample<Entity_>> createRightIterator(Value_ sliceValue) {
            var samples = pillarSourceInstance.samplingIterator(sliceValue, sampler, random);
            if (!samples.hasNext()) {
                // Refused by the sampler, or the slice is smaller than its minimumSize; try again,
                // and retire sliceValue for good once every attempt has failed.
                return Collections.emptyIterator();
            }
            return List.of(samples.next()).iterator();
        }

        @Override
        public void accept(Value_ sliceValue, Sample<Entity_> pillar) {
            nextMove = Moves.massChange(variableMetaModel, pillar, null);
        }

    }

}
