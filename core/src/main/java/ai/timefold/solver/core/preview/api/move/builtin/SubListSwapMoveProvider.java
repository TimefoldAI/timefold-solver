package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.ListCandidatePoolSource;
import ai.timefold.solver.core.impl.move.builtin.ListDatasetCandidateSource;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SubListSwapMoveIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;

/**
 * For two contiguous spans ("sub-lists") of assigned runs of a list variable,
 * each governed by its own size bounds,
 * creates a move to swap them,
 * possibly on different entities,
 * possibly in reverse element order.
 * <p>
 * There is no {@code crossingNull} flag:
 * a swap of two spans cannot cross null by construction,
 * since both spans are drawn from assigned runs.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListSwapMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final int leftMinimumSubListSize;
    private final int leftMaximumSubListSize;
    private final int rightMinimumSubListSize;
    private final int rightMaximumSubListSize;
    private final boolean selectReversingMoveToo;

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, 1, SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, 1,
                SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize) {
        this(variableMetaModel, minimumSubListSize, maximumSubListSize, minimumSubListSize, maximumSubListSize, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int leftMinimumSubListSize, int leftMaximumSubListSize, int rightMinimumSubListSize,
            int rightMaximumSubListSize) {
        this(variableMetaModel, leftMinimumSubListSize, leftMaximumSubListSize, rightMinimumSubListSize,
                rightMaximumSubListSize, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int leftMinimumSubListSize, int leftMaximumSubListSize, int rightMinimumSubListSize,
            int rightMaximumSubListSize, boolean selectReversingMoveToo) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        TriangleElementFactory.validateSizes(leftMinimumSubListSize, leftMaximumSubListSize);
        TriangleElementFactory.validateSizes(rightMinimumSubListSize, rightMaximumSubListSize);
        this.leftMinimumSubListSize = leftMinimumSubListSize;
        this.leftMaximumSubListSize = leftMaximumSubListSize;
        this.rightMinimumSubListSize = rightMinimumSubListSize;
        this.rightMaximumSubListSize = rightMaximumSubListSize;
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        var entityDataset = variableMetaModel.isValueRangeOnSolution() ? null
                : moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new SubListSwapMoveIterator<>(session, random,
                variableMetaModel, (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset),
                entityDataset == null
                        ? new ListDatasetCandidateSource<>(session.getSolutionView(), variableMetaModel, random, null,
                                session.getInstance(valueDataset))
                        : ListCandidatePoolSource.of(session.getSolutionView(), variableMetaModel, random,
                                session.getInstance(entityDataset), false),
                leftMinimumSubListSize,
                leftMaximumSubListSize,
                rightMinimumSubListSize, rightMaximumSubListSize, selectReversingMoveToo));
    }

    /**
     * @return {@code SubListSwapMoveProvider(<Entity>.<variable>,<minimum>-<maximum>,<minimum>-<maximum>,reversing|noReversing)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(SubListSwapMoveProvider.class, variableMetaModel,
                MoveProviderUtil.range(leftMinimumSubListSize, leftMaximumSubListSize),
                MoveProviderUtil.range(rightMinimumSubListSize, rightMaximumSubListSize),
                MoveProviderUtil.flag("reversing", selectReversingMoveToo));
    }

}
