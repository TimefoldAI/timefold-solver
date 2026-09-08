package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * For each entity whose basic planning variable is currently assigned (non-null),
 * creates a move to unassign it (set the variable to null).
 * <p>
 * This provider only applies to planning variables that allow unassigned values.
 * <p>
 * {@code ChangeMoveProvider} makes this same move too,
 * whenever its own {@code crossingNull} is {@code true} -
 * but there, only with probability {@code 1/(v+1)} per draw
 * (where {@code v} is the number of registered values),
 * so it arrives rarely.
 * This class exists to make it happen often.
 * <p>
 * For the complementary moves:
 * <ul>
 * <li>Use {@code AssignMoveProvider} to assign a value to currently-unassigned entities.</li>
 * <li>Use {@code ChangeMoveProvider} to change an entity's value to a different non-null value.</li>
 * </ul>
 * <p>
 * For unassigning several entities at once,
 * see {@code PillarUnassignMoveProvider} and {@code SubPillarUnassignMoveProvider} (members share a value)
 * or {@code MassUnassignMoveProvider} (members need not share anything).
 *
 * @see ChangeMoveProvider Changing a single entity to a different non-null value too, as one candidate among many.
 * @see AssignMoveProvider Assigning a value to currently-unassigned entities.
 * @see PillarUnassignMoveProvider Unassigning the whole pillar of every entity sharing a value at once.
 * @see SubPillarUnassignMoveProvider A sampler-driven subset of such a pillar.
 * @see MassUnassignMoveProvider A mixed-value sample with no shared key.
 */
@NullMarked
public final class UnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public UnassignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        return moveStreamFactory.pick(
                moveStreamFactory.forEach(variableMetaModel.entity().type(), false)
                        .filter((view, e) -> view.getValue(variableMetaModel, e) != null))
                .asMove((view, entity) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), null));
    }

}
