package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws contiguous spans ("sub-lists") of assigned runs of a list variable
 * and creates a move to unassign every member of the drawn span at once.
 * <p>
 * {@code SubListChangeMoveProvider} makes this same move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only as one destination row among many,
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * The variable must {@link PlanningListVariableMetaModel#allowsUnassignedValues() allow unassigned values};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @see SubListChangeMoveProvider Unassigning the whole span too, as one candidate among many.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final int minimumSubListSize;
    private final int maximumSubListSize;

    public SubListUnassignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, 2, SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE);
    }

    public SubListUnassignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
        // Size-1 spans are excluded: ListUnassignMoveProvider already covers them.
        TriangleElementFactory.validateSizes(minimumSubListSize, maximumSubListSize, 2);
        this.minimumSubListSize = minimumSubListSize;
        this.maximumSubListSize = maximumSubListSize;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new SubListUnassignMoveIterator<>(session, random,
                variableMetaModel, valueDataset, minimumSubListSize, maximumSubListSize));
    }

    /**
     * Draws a span sharing an assigned seed value and unassigns every member,
     * producing a {@code SubListUnassignMove}.
     * The destination is fixed at null, so nothing can ever be rejected:
     * no {@code RetiringBiWalk} is needed,
     * and a failed draw is proof that the seed's entity has fewer unpinned values than the minimum sub-list size,
     * not a bail-out false negative -
     * so the seed is retired immediately rather than probed N times.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class SubListUnassignMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>> {

        private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> valueIterator;
        private final SubListSampler<Solution_, Entity_, Value_> sampler;

        private @Nullable Move<Solution_> nextMove = null;

        SubListUnassignMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> valueDataset, int minimumSubListSize, int maximumSubListSize) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.solutionView = session.getSolutionView();
            var valueInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset);
            this.valueIterator = valueInstance.retiringRandomIterator(random);
            this.sampler = Samplers.subList(variableMetaModel, minimumSubListSize, maximumSubListSize, random);
        }

        @Override
        public boolean hasNext() {
            if (nextMove != null) {
                return true;
            }
            while (valueIterator.hasNext()) {
                var value = valueIterator.next();
                var range = sampler.byValue(solutionView, value);
                if (range == null) {
                    valueIterator.retire();
                    continue;
                }
                nextMove = Moves.unassign(variableMetaModel, range);
                return true;
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

    }

}
