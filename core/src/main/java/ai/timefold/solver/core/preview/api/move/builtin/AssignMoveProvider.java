package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.move.builtin.ValueRangeChangeMoveIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * Assigns a non-null value to each entity whose planning variable is currently unassigned (null).
 * Only generates moves where the value is in range for that entity.
 * <p>
 * {@code ChangeMoveProvider} makes this same move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only when the randomly drawn entity happens to already be unassigned,
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 *
 * @see ChangeMoveProvider Moving an already-assigned entity to a different value.
 * @see UnassignMoveProvider Moving an already-assigned entity to null (unassigning).
 * @see MassAssignMoveProvider Assigning several unassigned entities at once.
 */
@NullMarked
public final class AssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public AssignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var unassignedEntityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false)
                .filter((view, e) -> view.getValue(variableMetaModel, e) == null)
                .asCachedDataset();
        // Values come from the entity's own value range, so an out-of-range value is never drawn.
        return moveStreamFactory.buildMoveStream((session, random) -> {
            var entityInstance = (DefaultUniDatasetInstance<Solution_, Entity_>) session.getInstance(unassignedEntityDataset);
            return new ValueRangeChangeMoveIterator<>(variableMetaModel, session.getSolutionView(),
                    entityInstance.retiringRandomIterator(random), random, false);
        });
    }

    /**
     * @return {@code AssignMoveProvider(<Entity>.<variable>)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(AssignMoveProvider.class, variableMetaModel);
    }

}
