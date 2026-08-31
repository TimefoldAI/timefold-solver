package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For each subpillar of entities sharing a non-null value of the given variable,
 * governed by a {@link PillarSampler} -
 * {@code Samplers.pillar(Samplers.between(2, n))} is the recommended choice,
 * since an unbounded {@code Samplers.pillar(Samplers.all())} makes this provider's move cost linear in the pillar's size -
 * creates a move to change every member's value to a different value that is legal for every member.
 * The (sub)pillar is keyed on this one variable alone;
 * members may differ in every other variable.
 * The subpillar's own current value is never offered as a destination.
 * <p>
 * When {@code crossingNull} is {@code true}
 * (the default whenever the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned values}),
 * this provider also creates a move that unassigns the whole subpillar -
 * probability {@code 1/(s+1)} per drawn subpillar,
 * where {@code s} is the size of the subpillar members' value range.
 * This provider never assigns:
 * its key is a value shared by its members,
 * and unassigned is the absence of a value.
 *
 * @see SubPillarUnassignMoveProvider Unassigning the whole subpillar at a much higher rate.
 * @see MassAssignMoveProvider A sampler-chosen subset of the unassigned entities.
 * @see AssignMoveProvider Assigning a single entity at a time.
 * @see PillarChangeMoveProvider The whole pillar of every entity sharing the value at once.
 * @see MassChangeMoveProvider A mixed-value sample with no shared key, which may include unassigned entities.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubPillarChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final PillarSampler<Value_, Entity_> sampler;
    private final boolean crossingNull;

    public SubPillarChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            PillarSampler<Value_, Entity_> sampler) {
        this(variableMetaModel, sampler, variableMetaModel.allowsUnassigned());
    }

    /**
     * @param crossingNull if {@code true}, also creates whole-subpillar unassign moves;
     *        requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public SubPillarChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            PillarSampler<Value_, Entity_> sampler, boolean crossingNull) {
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
        var distinctValueDataset = MoveProviderUtil.distinctAssignedValues(moveStreamFactory, variableMetaModel);
        var pillarSourceDataset = MoveProviderUtil.entitiesByAssignedValue(moveStreamFactory, variableMetaModel);
        return moveStreamFactory.buildMoveStream((session, random) -> new SubPillarChangeMoveIterator<>(session, random,
                variableMetaModel, distinctValueDataset, pillarSourceDataset, sampler, crossingNull));
    }

    /**
     * Draws subpillars sharing an assigned value ("slice value")
     * and pairs each with a destination value, producing a {@code MassChangeMove}.
     * Left = slice value, right = destination value.
     * <p>
     * Unlike the whole-pillar variant,
     * a fresh subpillar is assembled on <strong>every</strong> {@link #createRightIterator} call, never cached across probes:
     * a real {@link PillarSampler} can legitimately draw a different subpillar for the same slice value on each attempt,
     * and caching the first one would turn {@link RetiringBiWalk}'s remaining probes into deterministic no-ops.
     * <p>
     * The destination is drawn from the (sub)pillar members' own {@link ValueRange}s
     * ({@link SampleValueRanges#findDestination}) rather than from a global candidate pool:
     * every candidate offered is already legal for every member and different from the slice value,
     * so no {@code isValueInRange} filtering or bail-out sampling is needed.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class SubPillarChangeMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, Value_> {

        private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final PillarSampler<Value_, Entity_> sampler;
        private final boolean crossingNull;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> sliceValueIterator;
        private final BiDatasetInstance<Value_, Entity_> pillarSourceInstance;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable Sample<Entity_> pendingPillar = null;

        public SubPillarChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> distinctValueDataset,
                BiDataset<Solution_, Value_, Entity_> pillarSourceDataset,
                PillarSampler<Value_, Entity_> sampler, boolean crossingNull) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.sampler = Objects.requireNonNull(sampler);
            this.crossingNull = crossingNull;
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
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
        public Iterator<Value_> createRightIterator(Value_ sliceValue) {
            // Fresh subpillar and fresh ranges on every call.
            var pillars = pillarSourceInstance.samplingIterator(sliceValue, sampler, random);
            var pillar = pillars.hasNext() ? pillars.next() : null;
            if (pillar == null || pillar.size() < 2) {
                // Size-1 subpillars are excluded from change: ChangeMoveProvider already covers them.
                // Empty rather than rejecting in acceptLeft,
                // which skips without retiring and would spin forever on a model where every subpillar has one member.
                pendingPillar = null;
                return Collections.emptyIterator();
            }
            var ranges = SampleValueRanges.of(pillar, variableMetaModel, solutionView);
            if (crossingNull && ranges.rollNull(random)) {
                pendingPillar = pillar;
                // List.of(null) throws; Collections.singletonList allows a null element.
                return Collections.singletonList((Value_) null).iterator();
            }
            var destination = ranges.findDestination(random, sliceValue);
            if (destination == null) {
                pendingPillar = null;
                return Collections.emptyIterator();
            }
            pendingPillar = pillar;
            return List.of(destination).iterator();
        }

        @Override
        public void accept(Value_ sliceValue, Value_ destination) {
            nextMove = Moves.massChange(variableMetaModel, Objects.requireNonNull(pendingPillar), destination);
            pendingPillar = null;
        }

    }

}
