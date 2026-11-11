package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.SubListChangeMoveIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;

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
        this(variableMetaModel, 2, SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, true,
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
        // Size-1 spans are excluded: ListChangeMoveProvider already covers them.
        TriangleElementFactory.validateSizes(minimumSubListSize, maximumSubListSize, 2);
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
        if (variableMetaModel.isValueRangeOnSolution()) {
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
                    variableMetaModel, crossingNull,
                    (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset),
                    session.getInstance(destinationDataset),
                    null, minimumSubListSize, maximumSubListSize, selectReversingMoveToo));
        }
        // Destinations come from the entities and values which share a value range with the span.
        var entityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new SubListChangeMoveIterator<>(session, random,
                variableMetaModel, crossingNull,
                (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset), null,
                session.getInstance(entityDataset), minimumSubListSize, maximumSubListSize, selectReversingMoveToo));
    }

    /**
     * @return {@code SubListChangeMoveProvider(<Entity>.<variable>,<minimum>-<maximum>,reversing|noReversing,crossingNull|noCrossingNull)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(SubListChangeMoveProvider.class, variableMetaModel,
                MoveProviderUtil.range(minimumSubListSize, maximumSubListSize),
                MoveProviderUtil.flag("reversing", selectReversingMoveToo),
                MoveProviderUtil.flag("crossingNull", crossingNull));
    }

}
