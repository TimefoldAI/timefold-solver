package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws {@link Sample}s of size 2 or more from {@code sourceDataset}, with no grouping key,
 * and pairs each with a destination position legal for every member,
 * producing a {@code MassListChangeMove}.
 * <p>
 * A plain {@link Iterator}, not a {@link RetiringBiWalk}: there is no left value to retire.
 * {@code samplingIterator} may end early -
 * the sampler can refuse a draw, or {@code sourceDataset} can be smaller than the sampler's minimum size -
 * and {@link #hasNext()} then simply ends too, with no hang,
 * since each call tries a fresh source.
 * It otherwise bounds itself with a flat {@link RetiringBiWalk#PROBE_ATTEMPT_COUNT} failed-draw budget.
 * The destination search itself is a separate, pool-scaled budget.
 * With entity value ranges, destinations come from {@link ListCandidatePool},
 * seeded by the sample member with the smallest pool.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class MassListDestinationMoveIterator<Solution_, Entity_, Value_> implements Iterator<Move<Solution_>> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final SolutionView<Solution_> solutionView;
    private final Iterator<Sample<Value_>> sampleIterator;
    private final @Nullable UniDatasetInstance<ElementPosition> destinationInstance;
    private final boolean includeUnassigned;
    // Null with a solution value range.
    private final @Nullable ListCandidatePool<Solution_, Entity_, Value_> candidatePool;
    private final RandomGenerator random;

    private @Nullable Move<Solution_> nextMove = null;

    public MassListDestinationMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniDataset<Solution_, Value_> sourceDataset, @Nullable UniDataset<Solution_, ElementPosition> destinationDataset,
            @Nullable UniDataset<Solution_, Entity_> entityDataset, boolean includeUnassigned, Sampler<Value_> sampler) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        var sourceInstance = session.getInstance(sourceDataset);
        this.sampleIterator = sourceInstance.samplingIterator(Objects.requireNonNull(sampler), random);
        this.destinationInstance = destinationDataset == null ? null : session.getInstance(destinationDataset);
        this.includeUnassigned = includeUnassigned;
        this.candidatePool = entityDataset == null ? null
                : ListCandidatePool.of(solutionView, variableMetaModel, random, session.getInstance(entityDataset));
    }

    @Override
    public boolean hasNext() {
        // sampleIterator.hasNext() can return false early - a sampler refusal, or sourceDataset
        // smaller than the sampler's minimum size - and the while condition below ends this call
        // right there, with no hang: each call tries a fresh source, independent of the last.
        // Otherwise, failed draws are counted
        // and this call gives up once they reach RetiringBiWalk.PROBE_ATTEMPT_COUNT.
        var failedSampleDraws = 0;
        while (nextMove == null && sampleIterator.hasNext() && failedSampleDraws < RetiringBiWalk.PROBE_ATTEMPT_COUNT) {
            var sample = sampleIterator.next();
            if (sample.size() < 2) {
                // Size-1 samples are excluded:
                // ListChangeMoveProvider/ListAssignMoveProvider already cover them, more cheaply than a full destination search would here.
                failedSampleDraws++;
                continue;
            }
            var destination = candidatePool != null
                    ? candidatePool.drawDestination(ListCandidatePool.narrowestValue(solutionView, variableMetaModel, sample),
                            includeUnassigned,
                            position -> isValidDestination(sample, position))
                    : AbstractLeftDrivenMoveIterator.drawMatching(Objects.requireNonNull(destinationInstance).iterator(random),
                            destinationInstance.size(),
                            position -> isValidDestination(sample, position));
            if (destination == null) {
                failedSampleDraws++;
                continue;
            }
            nextMove = (destination instanceof UnassignedElement)
                    ? Moves.massChange(variableMetaModel, sample, null)
                    : Moves.massChange(variableMetaModel, sample, (PositionInList) destination);
        }
        return nextMove != null;
    }

    private boolean isValidDestination(Sample<Value_> sample, ElementPosition destination) {
        if (destination instanceof UnassignedElement) {
            return true;
        }
        var assignedDestination = (PositionInList) destination;
        Entity_ destinationEntity = assignedDestination.entity();
        if (variableMetaModel.isValueRangeOnSolution()) {
            // We can move freely between entities, no per-entity value range to violate.
            return true;
        }
        return MoveProviderUtil.acceptsAll(solutionView, variableMetaModel, destinationEntity, sample);
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
