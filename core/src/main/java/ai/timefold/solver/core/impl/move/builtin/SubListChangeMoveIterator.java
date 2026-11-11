package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws a span sharing an assigned seed value and pairs it with a destination position,
 * producing a {@code SubListChangeMove}
 * or, for an unassigned destination, a {@code SubListUnassignMove}.
 * A fresh span is drawn for every seed value.
 * Each seed value draw tries one destination, from a pool which is the same for every span
 * (see {@link AbstractSingleDrawMoveIterator});
 * every valid pair of a span and a destination therefore has the same chance,
 * and short spans, which fit more destinations, are drawn more often than long ones.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListChangeMoveIterator<Solution_, Entity_, Value_>
        extends AbstractSingleDrawMoveIterator<Solution_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;
    private final boolean selectReversingMoveToo;
    private final RandomGenerator random;
    private final SolutionView<Solution_> solutionView;
    private final @Nullable UniDatasetInstance<ElementPosition> destinationInstance;
    private final @Nullable Iterator<ElementPosition> destinationIterator;
    private final SubListSampler<Solution_, Entity_, Value_> sampler;
    private final @Nullable ListCandidatePool<Solution_, Entity_, Value_> candidatePool;

    /**
     * @param destinationInstance null to draw destinations from {@link ListCandidatePool}
     * @param entityInstance the movable entities; null to draw destinations from the destination instance
     */
    public SubListChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, boolean crossingNull,
            DefaultUniDatasetInstance<Solution_, Value_> sourceInstance,
            @Nullable UniDatasetInstance<ElementPosition> destinationInstance,
            @Nullable UniDatasetInstance<Entity_> entityInstance, int minimumSubListSize,
            int maximumSubListSize, boolean selectReversingMoveToo) {
        super(sourceInstance.retiringRandomIterator(random));
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.crossingNull = crossingNull;
        this.selectReversingMoveToo = selectReversingMoveToo;
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        this.destinationInstance = destinationInstance;
        this.destinationIterator = destinationInstance == null ? null : destinationInstance.iterator(random);
        this.sampler = Samplers.subList(variableMetaModel, minimumSubListSize, maximumSubListSize, random);
        this.candidatePool = entityInstance == null ? null
                : ListCandidatePool.of(solutionView, variableMetaModel, random, entityInstance);
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Value_ sliceValue) {
        var range = sampler.byValue(solutionView, sliceValue);
        if (range == null) {
            return null;
        }
        ElementPosition destination;
        if (candidatePool != null) {
            destination = candidatePool.drawDestinationOnce(crossingNull, position -> isValidChange(range, position));
        } else {
            var iterator = Objects.requireNonNull(destinationIterator);
            destination = iterator.hasNext() ? iterator.next() : null;
            if (destination != null && !isValidChange(range, destination)) {
                destination = null;
            }
        }
        return destination == null ? null : buildMove(range, destination);
    }

    @Override
    protected boolean hasMove(Value_ sliceValue) {
        // A span can be invalid while another span of the same seed value is valid.
        for (var spanAttempt = 0; spanAttempt < FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER; spanAttempt++) {
            var range = sampler.byValue(solutionView, sliceValue);
            if (range == null) {
                return false;
            }
            ElementPosition destination;
            if (candidatePool != null) {
                // Only the entities which accept the narrowest value can accept the span; the budget fits that pool.
                var narrowestValue = candidatePool.narrowestValue(range.entity(), range.fromIndex(), range.toIndex());
                destination = candidatePool.drawDestination(narrowestValue, crossingNull,
                        position -> isValidChange(range, position));
            } else {
                destination = drawMatching(Objects.requireNonNull(destinationIterator),
                        Objects.requireNonNull(destinationInstance).size(), position -> isValidChange(range, position));
            }
            if (destination != null) {
                return true;
            }
        }
        return false;
    }

    private Move<Solution_> buildMove(Range<Entity_> range, ElementPosition destination) {
        if (destination instanceof UnassignedElement) {
            return Moves.unassign(variableMetaModel, range);
        }
        var reversing = selectReversingMoveToo && range.length() > 1 && random.nextBoolean();
        return Moves.change(variableMetaModel, range, (PositionInList) destination, reversing);
    }

    private boolean isValidChange(Range<Entity_> range, ElementPosition destination) {
        if (destination instanceof UnassignedElement) {
            return true;
        }
        var assignedDestination = (PositionInList) destination;
        var sourceEntity = range.entity();
        Entity_ destinationEntity = assignedDestination.entity();
        if (sourceEntity == destinationEntity) {
            return assignedDestination.index() != range.fromIndex()
                    && assignedDestination.index() + range.length() <= solutionView.countValues(variableMetaModel,
                            sourceEntity);
        }
        if (variableMetaModel.isValueRangeOnSolution()) {
            // We can move freely between entities, no per-entity value range to violate.
            return true;
        }
        return MoveProviderUtil.acceptsAll(solutionView, variableMetaModel, destinationEntity, sourceEntity,
                range.fromIndex(), range.toIndex());
    }

}
