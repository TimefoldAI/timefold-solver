package ai.timefold.solver.core.impl.move.builtin;

import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws two spans sharing no particular relationship other than both being seeded by an assigned value,
 * producing a {@code SubListSwapMove}.
 * A fresh left span is drawn for every seed value, and a fresh right span for every candidate probed.
 * Each left seed value draw tries one right seed, from a pool which is the same for every left span
 * (see {@link AbstractSingleDrawMoveIterator});
 * every valid pair of spans therefore has the same chance,
 * and short spans, which fit more partners, are drawn more often than long ones.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListSwapMoveIterator<Solution_, Entity_, Value_>
        extends AbstractSingleDrawMoveIterator<Solution_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean selectReversingMoveToo;
    private final RandomGenerator random;
    private final SolutionView<Solution_> solutionView;
    private final SubListSampler<Solution_, Entity_, Value_> leftSampler;
    private final SubListSampler<Solution_, Entity_, Value_> rightSampler;
    private final ListCandidateSource<Entity_, Value_> candidateSource;

    /**
     * @param valueInstance the seed values of the left spans
     * @param candidateSource the seed values of the right spans
     */
    public SubListSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            DefaultUniDatasetInstance<Solution_, Value_> valueInstance,
            ListCandidateSource<Entity_, Value_> candidateSource,
            int leftMinimumSubListSize, int leftMaximumSubListSize,
            int rightMinimumSubListSize, int rightMaximumSubListSize, boolean selectReversingMoveToo) {
        super(valueInstance.retiringRandomIterator(random));
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.selectReversingMoveToo = selectReversingMoveToo;
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        this.leftSampler = Samplers.subList(variableMetaModel, leftMinimumSubListSize, leftMaximumSubListSize, random);
        this.rightSampler =
                Samplers.subList(variableMetaModel, rightMinimumSubListSize, rightMaximumSubListSize, random);
        this.candidateSource = Objects.requireNonNull(candidateSource);
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Value_ leftValue) {
        var leftRange = leftSampler.byValue(solutionView, leftValue);
        if (leftRange == null) {
            return null;
        }
        var rightValue = candidateSource.drawPartnerOnce(value -> true);
        if (rightValue == null) {
            return null;
        }
        var rightRange = rightSampler.byValue(solutionView, rightValue);
        if (rightRange == null || !isValidSwap(leftRange, rightRange)) {
            return null;
        }
        var reversing =
                selectReversingMoveToo && (leftRange.length() > 1 || rightRange.length() > 1) && random.nextBoolean();
        return Moves.swap(variableMetaModel, leftRange, rightRange, reversing);
    }

    @Override
    protected boolean hasMove(Value_ leftValue) {
        // A left span can be invalid while another left span of the same seed value is valid.
        for (var spanAttempt = 0; spanAttempt < FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER; spanAttempt++) {
            var leftRange = leftSampler.byValue(solutionView, leftValue);
            if (leftRange == null) {
                return false;
            }
            // Only the entities which accept the narrowest value can accept the left span; the budget fits that pool.
            var narrowestValue = candidateSource.narrowestValue(leftRange.entity(), leftRange.fromIndex(), leftRange.toIndex());
            var rightValue = candidateSource.drawPartner(narrowestValue, candidateValue -> {
                var rightRange = rightSampler.byValue(solutionView, candidateValue);
                return rightRange != null && isValidSwap(leftRange, rightRange);
            });
            if (rightValue != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidSwap(Range<Entity_> left, Range<Entity_> right) {
        var leftEntity = left.entity();
        var rightEntity = right.entity();
        if (leftEntity == rightEntity) {
            // Must not overlap.
            return left.toIndex() <= right.fromIndex() || right.toIndex() <= left.fromIndex();
        }
        if (variableMetaModel.isValueRangeOnSolution()) {
            return true;
        }
        return MoveProviderUtil.acceptsAll(solutionView, variableMetaModel, rightEntity, leftEntity, left.fromIndex(),
                left.toIndex())
                && MoveProviderUtil.acceptsAll(solutionView, variableMetaModel, leftEntity, rightEntity, right.fromIndex(),
                        right.toIndex());
    }

}
