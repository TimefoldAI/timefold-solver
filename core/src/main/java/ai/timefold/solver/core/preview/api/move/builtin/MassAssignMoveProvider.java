package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.move.builtin.SampleValueRanges;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
        return moveStreamFactory.buildMoveStream((session, random) -> new MassAssignMoveIterator<>(session, random,
                variableMetaModel, unassignedEntityDataset, sampler));
    }

    private static final class MassAssignMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>> {

        private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final SolutionView<Solution_> solutionView;
        private final RandomGenerator random;
        private final Iterator<Sample<Entity_>> sampleIterator;

        private @Nullable Move<Solution_> nextMove = null;
        /**
         * Remembers the last distinct ranges proven to have no legal destination,
         * so that redrawing an equal-signature sample does not repeat the exhaustive proof on every failed draw.
         */
        private @Nullable SampleValueRanges<Value_> provenEmptyRanges = null;

        MassAssignMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Entity_> sourceDataset, Sampler<Entity_> sampler) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            var sourceInstance = session.getInstance(sourceDataset);
            this.sampleIterator = sourceInstance.samplingIterator(Objects.requireNonNull(sampler), random);
        }

        @Override
        public boolean hasNext() {
            var failedSampleDraws = 0;
            while (nextMove == null && sampleIterator.hasNext() && failedSampleDraws < RetiringBiWalk.PROBE_ATTEMPT_COUNT) {
                var sample = sampleIterator.next();
                if (sample.size() < 2) {
                    failedSampleDraws++;
                    continue;
                }
                var ranges = SampleValueRanges.of(sample, variableMetaModel, solutionView);
                if (Objects.equals(ranges, provenEmptyRanges)) {
                    // Already proven empty for this exact signature; no need to search again.
                    failedSampleDraws++;
                    continue;
                }
                var targetValue = ranges.findTarget(random, null);
                if (targetValue == null) {
                    provenEmptyRanges = ranges;
                    failedSampleDraws++;
                    continue;
                }
                nextMove = Moves.massChange(variableMetaModel, sample, targetValue);
            }
            return nextMove != null;
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

    }

}
