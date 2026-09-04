package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SampleValueRanges;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.SingletonIterator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For each pillar of entities sharing a non-null value of the given variable, creates a move to change every member's
 * value to a different value that is legal for every member.
 * The pillar is keyed on this one variable alone; members may differ in every other variable.
 * The pillar's own current value is never offered as a destination.
 * <p>
 * When {@code crossingNull} is {@code true}
 * (the default whenever the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned values}),
 * this provider also creates a move that unassigns the whole pillar - probability {@code 1/(s+1)} per drawn pillar,
 * where {@code s} is the size of the pillar members' value range.
 * This provider never assigns: a pillar's key is a value shared by its members, and unassigned is the absence of a
 * value, not one.
 * Use {@code PillarUnassignMoveProvider} for unassign moves at a much higher rate;
 * there is no pillar equivalent for assign, since it would require drawing a pillar keyed on "unassigned".
 * <p>
 * Draws whole pillars only, unbounded by design: a pillar move is defined as moving every member of the pillar,
 * so its cost is linear in the pillar's size with no cap.
 *
 * @see PillarUnassignMoveProvider Unassigning the whole pillar at a much higher rate.
 * @see MassAssignMoveProvider A sampler-chosen subset of the unassigned entities.
 * @see AssignMoveProvider Assigning a single entity at a time.
 * @see SubPillarChangeMoveProvider A sampler-driven, size-bounded subset of the pillar.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class PillarChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;

    public PillarChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, variableMetaModel.allowsUnassigned());
    }

    /**
     * @param crossingNull if {@code true}, also creates whole-pillar unassign moves; requires that the variable
     *        {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public PillarChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
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
        var pillarDataset = MoveProviderUtil.assignedEntities(moveStreamFactory, variableMetaModel)
                .groupBy((solutionView, entity) -> solutionView.getValue(variableMetaModel, entity),
                        NeighborhoodsCollectors.collectAndThen(NeighborhoodsCollectors.toList(), Sample::of))
                .map((solutionView, value, pillar) -> new PillarWithRange<>(pillar,
                        SampleValueRanges.of(pillar, variableMetaModel, solutionView)))
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new PillarChangeMoveIterator<>(session, random,
                variableMetaModel, pillarDataset, crossingNull));
    }

    /**
     * A pillar bundled with its {@link SampleValueRanges}, computed once when the {@code .map(...)} step settles for a
     * dirty group (see {@link #build}), not once per redraw across local search steps.
     */
    private record PillarWithRange<Entity_, Value_>(Sample<Entity_> pillar, SampleValueRanges<Value_> ranges) {
    }

    /**
     * Draws whole pillars (one cached row per assigned value) and pairs each with a destination value,
     * producing a {@code MassChangeMove}.
     * Left = pillar, right = destination value.
     * <p>
     * The destination is drawn from the pillar members' own {@link ValueRange}s
     * ({@link SampleValueRanges#findDestination}) rather than from a global candidate pool: every candidate offered is
     * already legal for every member and different from the pillar's own value, so no {@code isValueInRange} filtering
     * or bail-out sampling is needed.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class PillarChangeMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<PillarWithRange<Entity_, Value_>, Value_> {

        private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final boolean crossingNull;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<PillarWithRange<Entity_, Value_>> sampleIterator;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable Sample<Entity_> cachedPillar = null;
        /**
         * Scoped to one step's up-to-3 {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} probes of the same left pillar,
         * reset whenever {@link #cachedPillar} changes - unlike the ranges themselves
         * (now precomputed on {@link PillarWithRange}, settle-cached, and shared across every step until the pillar's
         * group changes), this latch is cheap to recompute, so caching it across steps isn't worth the extra bookkeeping.
         * Compared by {@code .pillar()}, not whole-record equality, since a record's auto-generated equals would also
         * compare {@code ranges} for no reason.
         * <p>
         * Also safe without the slice value in its key, unlike {@code MassDestinationMoveIterator}'s equivalent memo:
         * {@link MoveProviderUtil#assignedEntities} groups entities by their assigned value,
         * so a pillar's members determine its slice value, and an equal pillar therefore has an equal slice value too.
         */
        private boolean cachedPillarProvenEmpty = false;

        public PillarChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, PillarWithRange<Entity_, Value_>> pillarDataset, boolean crossingNull) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.crossingNull = crossingNull;
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            var pillarInstance =
                    (DefaultUniDatasetInstance<Solution_, PillarWithRange<Entity_, Value_>>) session.getInstance(pillarDataset);
            this.sampleIterator = pillarInstance.retiringRandomIterator(random);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(sampleIterator, this);
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
        public Iterator<Value_> createRightIterator(PillarWithRange<Entity_, Value_> pillarEntry) {
            var pillar = pillarEntry.pillar();
            if (pillar.size() < 2) {
                // Size-1 pillars are excluded from change: ChangeMoveProvider already covers them.

                return Collections.emptyIterator();
            }
            if (!Objects.equals(cachedPillar, pillar)) {
                cachedPillar = pillar;
                cachedPillarProvenEmpty = false;
            }
            var ranges = pillarEntry.ranges();
            // A pillar whose non-null intersection is empty still has a legal null destination (unassign),
            // so the null branch is tried first and also whenever the latch below is set.

            if (crossingNull && (cachedPillarProvenEmpty || ranges.rollNull(random))) {
                // List.of(null) throws; Collections.singletonList allows a null element.

                return new SingletonIterator<>(null);
            }
            if (cachedPillarProvenEmpty) {
                // Already proven empty for this pillar; no need to search again.
                return Collections.emptyIterator();
            }
            // The pillar is homogeneous by construction (one cached row per assigned value);
            // recover the slice value from any one member to exclude it as a no-op destination.
            var sliceValue = solutionView.getValue(variableMetaModel, Objects.requireNonNull(pillar.representative()));
            var destination = ranges.findDestination(random, sliceValue);
            if (destination == null) {
                cachedPillarProvenEmpty = true;
                return Collections.emptyIterator();
            }
            return new SingletonIterator<>(destination);
        }

        @Override
        public void accept(PillarWithRange<Entity_, Value_> pillarEntry, Value_ destination) {
            nextMove = Moves.massChange(variableMetaModel, pillarEntry.pillar(), destination);
        }

    }

}
