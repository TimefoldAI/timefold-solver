package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

/**
 * For each entity with a non-null value,
 * creates a move to change it to a different non-null value.
 * <p>
 * When {@code crossingNull} is {@code true}
 * (the default whenever the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned values}),
 * this provider also creates null-to-non-null (assign) and non-null-to-null (unassign) moves.
 * This does not remove the need for {@code AssignMoveProvider} and {@code UnassignMoveProvider}:
 * here, a null-crossing move is one candidate among many, so it arrives rarely -
 * an unassign draw has probability {@code 1/(v+1)} where {@code v} is the number of registered values,
 * and an assign draw only when the randomly drawn entity happens to already be unassigned.
 * A configuration that wants such moves often should add {@code AssignMoveProvider}/{@code UnassignMoveProvider}
 * in addition to turning this flag off to avoid further oversampling.
 *
 * @see AssignMoveProvider Assigning a single entity at a time.
 * @see UnassignMoveProvider Unassigning a single entity at a time.
 * @see PillarChangeMoveProvider Changing several entities at once, keyed on a shared value.
 * @see SubPillarChangeMoveProvider A sampler-driven subset of such a pillar.
 * @see MassChangeMoveProvider A mixed-value sample with no shared key, which may include unassigned entities.
 */
@NullMarked
public final class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, variableMetaModel.allowsUnassigned());
    }

    /**
     * @param crossingNull if {@code true}, also creates assign and unassign moves;
     *        requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (crossingNull && !variableMetaModel.allowsUnassigned()) {
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
        var nodeSharingSupportFunctions =
                ((DefaultMoveStreamFactory<Solution_>) moveStreamFactory).getNodeSharingSupportFunctions(variableMetaModel);
        var entities = moveStreamFactory.forEach(variableMetaModel.entity().type(), false);
        if (!crossingNull && variableMetaModel.allowsUnassigned()) {
            entities = entities.filter(nodeSharingSupportFunctions.assignedValueFilter());
        }
        return moveStreamFactory.pick(entities)
                .pick(moveStreamFactory.forEach(variableMetaModel.type(), crossingNull),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.differentValueFilter()),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilter()))
                .asMove((solution, entity, value) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), value));
    }

}
