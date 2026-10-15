package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
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
 * {@link MassChangeMoveIterator#sharedValueOf} excludes a destination only when
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
                ? allEntities(moveStreamFactory, variableMetaModel)
                : MoveProviderUtil.assignedEntityDataset(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new MassChangeMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, sampler, crossingNull));
    }

    /**
     * Every entity of the class, assigned or not, deliberately admitting unassigned entities:
     * a {@code Mass*} sample drawn from it may contain them,
     * and the move built from that sample assigns them as a side effect, crossing null upward.
     */
    private static <Solution_, Entity_, Value_> UniDataset<Solution_, Entity_> allEntities(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
    }

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
     * so {@link SampleValueRanges#findTarget} accepts the first candidate;
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
    private static final class MassChangeMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>> {

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

        MassChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
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
                var sharedValue = sharedValueOf(sample, variableMetaModel, solutionView);
                var ranges = SampleValueRanges.of(sample, variableMetaModel, solutionView);
                var provenEmptyForThisSignature = Objects.equals(sharedValue, valueWithNoLegalDestination) &&
                        Objects.equals(ranges, provenEmptyRanges);
                // A sample whose non-null intersection is empty (or already proven so)
                // still has a legal null destination (unassign), as long as it holds at least one entity to unassign.
                // Tried before the latch-based skip below, and also taken whenever the latch is already set.
                if (crossingNull
                        && (provenEmptyForThisSignature || ranges.rollNull(random))
                        && anyAssigned(sample, variableMetaModel, solutionView)) {
                    nextMove = Moves.massChange(variableMetaModel, sample, null);
                    continue;
                }
                if (provenEmptyForThisSignature) {
                    // Already proven empty for this exact signature; no need to search again.
                    failedSampleDraws++;
                    continue;
                }
                var targetValue = ranges.findTarget(random, sharedValue);
                if (targetValue == null) {
                    provenEmptyRanges = ranges;
                    valueWithNoLegalDestination = sharedValue;
                    failedSampleDraws++;
                    continue;
                }
                nextMove = Moves.massChange(variableMetaModel, sample, targetValue);
            }
            return nextMove != null;
        }

        /**
         * @return {@code true} if at least one member of {@code sample} currently holds a non-null value;
         *         short-circuits on the first one. Used to keep a null destination from being offered for a sample
         *         that is already entirely unassigned, which would otherwise be a no-op move.
         */
        private static <Solution_, Entity_, Value_> boolean anyAssigned(Sample<Entity_> sample,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, SolutionView<Solution_> solutionView) {
            for (var entity : sample) {
                if (solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity)) != null) {
                    return true;
                }
            }
            return false;
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

        /**
         * @return the value every member of {@code sample} currently holds,
         *         or {@code null} if any two members disagree,
         *         or if {@code sample} is entirely unassigned.
         *         Either answer is the correct {@code excludedValue} for {@link SampleValueRanges#findTarget}:
         *         {@code null} is never itself a candidate destination,
         *         so excluding "no shared value" excludes nothing.
         */
        private static <Solution_, Entity_, Value_> @Nullable Value_ sharedValueOf(Sample<Entity_> sample,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, SolutionView<Solution_> solutionView) {
            Value_ sharedValue = null;
            var first = true;
            for (var entity : sample) {
                var value = solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity));
                if (first) {
                    sharedValue = value;
                    first = false;
                } else { // Avoiding a megamorphic Objects.equals() on the hot path.
                    var equalValue = (sharedValue == value) || (sharedValue != null && sharedValue.equals(value));
                    if (!equalValue) {
                        return null;
                    }
                }
            }
            return sharedValue;
        }

    }

}
