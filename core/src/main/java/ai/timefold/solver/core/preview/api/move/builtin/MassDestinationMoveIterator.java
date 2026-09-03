package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws {@link Sample}s of size 2 or more from {@code sourceDataset},
 * with no grouping key,
 * and pairs each with a destination value legal for every member,
 * producing a {@code MassChangeMove}.
 * <p>
 * A plain {@link Iterator}, not a {@link RetiringBiWalk}:
 * there is no left value to retire.
 * {@code samplingIterator} may end early -
 * the sampler can refuse a draw, or {@code sourceDataset} can be smaller than the sampler's minimum size -
 * and {@link #hasNext()} then simply ends too,
 * with no hang, since each call tries a fresh source.
 * It otherwise bounds itself with a flat {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} failed-draw budget,
 * matching the fixed-width-probe design of the pillar family.
 * This is not neutral to every model, but it costs nothing on the two that matter:
 * a solution-wide {@link ValueRange} is one deduplicated range,
 * so {@link SampleValueRanges#findDestination} accepts the first candidate;
 * overlapping entity-dependent ranges normally intersect too,
 * so the first draw succeeds there as well.
 * Only disjoint entity-dependent ranges fail systematically,
 * and there the legal samples are so rare
 * (about {@code r^(1-k)} for r regions and samples of size k)
 * that a pool-scaled budget would pay for {@code n * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER} intersections every step
 * to recover a fraction of them.
 * That configuration wants {@link SubPillarChangeMoveProvider} or a region-aware {@link Sampler} instead.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
final class MassDestinationMoveIterator<Solution_, Entity_, Value_> implements Iterator<Move<Solution_>> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;
    private final SolutionView<Solution_> solutionView;
    private final RandomGenerator random;
    private final Iterator<Sample<Entity_>> sampleIterator;

    private @Nullable Move<Solution_> nextMove = null;
    /**
     * Remembers the last distinct ranges and value proven to have no legal destination,
     * so that redrawing an equal-signature sample
     * (possible even under a real {@link Sampler},
     * since two different draws can land on the same distinct ranges and the same value)
     * does not repeat the exhaustive proof on every failed draw.
     * Keyed on both fields together:
     * the same ranges can be empty for one value and non-empty for another.
     */
    private @Nullable SampleValueRanges<Value_> provenEmptyRanges = null;
    private @Nullable Value_ valueWithNoLegalDestination = null;

    MassDestinationMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniDataset<Solution_, Entity_> sourceDataset, Sampler<Entity_> sampler, boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.crossingNull = crossingNull;
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        var sourceInstance = session.getInstance(sourceDataset);
        this.sampleIterator = sourceInstance.samplingIterator(Objects.requireNonNull(sampler), random);
    }

    @Override
    public boolean hasNext() {
        // sampleIterator.hasNext() can return false early -
        // a sampler refusal, or sourceDataset smaller than the sampler's minimum size -
        // and the while condition below ends this call right there, with no hang:
        // each call tries a fresh source, independent of the last.
        // Otherwise, a real Sampler can draw a different sample on every call,
        // so failed draws are counted and this call gives up once they reach RetiringBiWalk.PROBE_ATTEMPT_COUNT.
        var failedSampleDraws = 0;
        while (nextMove == null && sampleIterator.hasNext() && failedSampleDraws < RetiringBiWalk.PROBE_ATTEMPT_COUNT) {
            var sample = sampleIterator.next();
            if (sample.size() < 2) {
                // Size-1 samples are excluded: ChangeMoveProvider/AssignMoveProvider already cover them,
                // more cheaply than a full range intersection and destination probe would here.
                failedSampleDraws++;
                continue;
            }
            var sharedValue = MoveProviderUtil.sharedValueOf(sample, variableMetaModel, solutionView);
            var ranges = SampleValueRanges.of(sample, variableMetaModel, solutionView);
            var provenEmptyForThisSignature =
                    Objects.equals(sharedValue, valueWithNoLegalDestination) && Objects.equals(ranges, provenEmptyRanges);
            // A sample whose non-null intersection is empty (or already proven so)
            // still has a legal null destination (unassign), as long as it holds at least one entity to unassign.
            // Tried before the latch-based skip below, and also taken whenever the latch is already set.
            if (crossingNull
                    && (provenEmptyForThisSignature || ranges.rollNull(random))
                    && MoveProviderUtil.anyAssigned(sample, variableMetaModel, solutionView)) {
                nextMove = Moves.massChange(variableMetaModel, sample, null);
                continue;
            }
            if (provenEmptyForThisSignature) {
                // Already proven empty for this exact signature; no need to search again.
                failedSampleDraws++;
                continue;
            }
            var destination = ranges.findDestination(random, sharedValue);
            if (destination == null) {
                provenEmptyRanges = ranges;
                valueWithNoLegalDestination = sharedValue;
                failedSampleDraws++;
                continue;
            }
            nextMove = Moves.massChange(variableMetaModel, sample, destination);
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
