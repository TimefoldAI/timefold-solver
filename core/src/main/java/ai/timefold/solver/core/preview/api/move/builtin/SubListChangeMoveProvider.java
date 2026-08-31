package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For each contiguous span ("sub-list") of an assigned run of a list variable,
 * creates a move to relocate it to a different position,
 * possibly on a different entity,
 * possibly in reverse element order.
 * <p>
 * When {@code crossingNull} is {@code true},
 * this provider also creates a move that unassigns the whole drawn span -
 * one destination row among many, so it arrives rarely.
 * Use {@code SubListUnassignMoveProvider} for unassign moves at a much higher rate.
 * <p>
 * This provider never assigns:
 * a drawn span's identity is a contiguous run of positions,
 * which the unassigned pool does not have.
 * For a set of unassigned values drawn together,
 * see {@code MassListAssignMoveProvider} instead.
 *
 * @see SubListUnassignMoveProvider Unassigning the whole span at a much higher rate.
 * @see MassListAssignMoveProvider A set of unassigned values drawn together.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final int minimumSubListSize;
    private final int maximumSubListSize;
    private final boolean selectReversingMoveToo;
    private final boolean crossingNull;

    public SubListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, 1, SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, true,
                variableMetaModel.allowsUnassignedValues());
    }

    public SubListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize) {
        this(variableMetaModel, minimumSubListSize, maximumSubListSize, true, variableMetaModel.allowsUnassignedValues());
    }

    /**
     * @param crossingNull if {@code true}, also creates whole-span unassign moves;
     *        requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned
     *        values},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public SubListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize, boolean selectReversingMoveToo, boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        TriangleElementFactory.validateSizes(minimumSubListSize, maximumSubListSize);
        this.minimumSubListSize = minimumSubListSize;
        this.maximumSubListSize = maximumSubListSize;
        this.selectReversingMoveToo = selectReversingMoveToo;
        if (crossingNull && !variableMetaModel.allowsUnassignedValues()) {
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
        var sourceDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        // Only widen to forEachDestinationIncludingUnassigned when crossingNull:
        // unlike forEachDestination, it represents the unassigned destination with a null entity internally,
        // which entity-provided value ranges cannot resolve -
        // avoid tripping that path when this provider has no use for it anyway.
        var destinationDataset = (crossingNull
                ? moveStreamFactory.forEachDestinationIncludingUnassigned(variableMetaModel)
                : moveStreamFactory.forEachDestination(variableMetaModel)
                        .map((solutionView, position) -> (ElementPosition) position))
                .asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new SubListChangeMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, destinationDataset, minimumSubListSize, maximumSubListSize,
                selectReversingMoveToo));
    }

    /**
     * Draws a span sharing an assigned seed value and pairs it with a destination position,
     * producing a {@code SubListChangeMove}
     * or, for an unassigned destination, a {@code SubListUnassignMove}.
     * Left = seed value, right = destination position.
     * <p>
     * A fresh span is drawn on <strong>every</strong> {@link #createRightIterator} call, never cached across probes:
     * caching the first draw would turn {@link RetiringBiWalk}'s remaining probes into deterministic no-ops,
     * the same reasoning {@link SubPillarChangeMoveProvider} documents.
     * <p>
     * <strong>Known ceiling:</strong> the left pool is seed values, but a value only picks its entity,
     * so deadness is per-entity while retirement is per-value.
     * An entity holding {@code k} unpinned values is reachable via {@code k} distinct left rows,
     * so a genuinely dead entity costs up to {@code 3k} probes before its last seed retires.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class SubListChangeMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, ElementPosition> {

        private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final boolean selectReversingMoveToo;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> sliceValueIterator;
        private final UniDatasetInstance<ElementPosition> destinationInstance;
        private final SubListSampler<Solution_, Entity_, Value_> sampler;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable Range<Entity_> pendingRange = null;

        SubListChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> sourceDataset, UniDataset<Solution_, ElementPosition> destinationDataset,
                int minimumSubListSize, int maximumSubListSize, boolean selectReversingMoveToo) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.selectReversingMoveToo = selectReversingMoveToo;
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            var sourceInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset);
            this.sliceValueIterator = sourceInstance.retiringRandomIterator(random);
            this.destinationInstance = session.getInstance(destinationDataset);
            this.sampler = Samplers.subList(variableMetaModel, minimumSubListSize, maximumSubListSize, random);
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
        public Iterator<ElementPosition> createRightIterator(Value_ sliceValue) {
            // Fresh span on every call; see the class javadoc.
            var range = sampler.byValue(solutionView, sliceValue);
            if (range == null) {
                pendingRange = null;
                return Collections.emptyIterator();
            }
            pendingRange = range;
            var bailOutSize = destinationInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(destinationInstance.iterator(random),
                    destination -> isValidChange(range, destination), bailOutSize);
        }

        private boolean isValidChange(Range<Entity_> range, ElementPosition destination) {
            if (destination instanceof UnassignedElement) {
                return true;
            }
            var targetAssigned = (PositionInList) destination;
            Entity_ sourceEntity = range.entity();
            Entity_ destinationEntity = targetAssigned.entity();
            if (sourceEntity == destinationEntity) {
                return targetAssigned.index() != range.fromIndex()
                        && targetAssigned.index() + range.length() <= solutionView.countValues(variableMetaModel,
                                sourceEntity);
            }
            if (variableMetaModel.isValueRangeOnSolution()) {
                // We can move freely between entities, no per-entity value range to violate.
                return true;
            }
            for (var index = range.fromIndex(); index < range.toIndex(); index++) {
                var value = solutionView.getValueAtIndex(variableMetaModel, sourceEntity, index);
                if (!solutionView.isValueInRange(variableMetaModel, destinationEntity, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void accept(Value_ sliceValue, ElementPosition destination) {
            var range = Objects.requireNonNull(pendingRange);
            pendingRange = null;
            if (destination instanceof UnassignedElement) {
                nextMove = Moves.unassign(variableMetaModel, range);
            } else {
                var reversing = selectReversingMoveToo && range.length() > 1 && random.nextBoolean();
                nextMove = Moves.change(variableMetaModel, range, (PositionInList) destination, reversing);
            }
        }

    }

}
