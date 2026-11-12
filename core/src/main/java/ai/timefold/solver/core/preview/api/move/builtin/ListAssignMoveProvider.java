package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.AbstractRepeatedDrawMoveIterator;
import ai.timefold.solver.core.impl.move.builtin.ListCandidatePool;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Assigns each unassigned value to a position in some entity's list variable.
 * Only generates moves where the value is in range for the destination entity.
 * <p>
 * Requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned values};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @see ListChangeMoveProvider Moving an already-assigned value to a different position.
 * @see ListUnassignMoveProvider Removing an already-assigned value from its position.
 * @see MassListAssignMoveProvider A set of unassigned values drawn together.
 */
@NullMarked
public final class ListAssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListAssignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        if (variableMetaModel.isValueRangeOnSolution()) {
            // Every destination accepts every value, so any pair is a valid move.
            return moveStreamFactory.pick(MoveProviderUtil.inValueRange(moveStreamFactory, variableMetaModel,
                    moveStreamFactory.forEachUnassignedValue(variableMetaModel)))
                    .pick(moveStreamFactory.forEachDestination(variableMetaModel))
                    .asMove((view, value, pos) -> Moves.assign(variableMetaModel, value, pos));
        }
        // Destinations come from the entities and values which share a value range with the value.
        var valueDataset = moveStreamFactory.forEachUnassignedValue(variableMetaModel).asCachedDataset();
        var entityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> {
            var valueInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset);
            var candidatePool = ListCandidatePool.of(session.getSolutionView(), variableMetaModel, random,
                    session.getInstance(entityDataset), false);
            return new AbstractRepeatedDrawMoveIterator<>(valueInstance.retiringRandomIterator(random)) {
                @Override
                protected @Nullable Move<Solution_> tryBuildMove(Value_ value) {
                    var destination = candidatePool.drawDestination(value, position -> true);
                    return destination == null ? null
                            : Moves.assign(variableMetaModel, value, (PositionInList) destination);
                }
            };
        });
    }

    /**
     * @return {@code ListAssignMoveProvider(<Entity>.<variable>)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(ListAssignMoveProvider.class, variableMetaModel);
    }

}
