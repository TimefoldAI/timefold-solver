package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListChangeMoveIterator<Solution_, Entity_, Value_>
        extends AbstractRepeatedDrawMoveIterator<Solution_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;
    private final RandomGenerator random;
    private final SolutionView<Solution_> solutionView;
    private final @Nullable UniDatasetInstance<ElementPosition> destinationInstance;
    private final @Nullable Iterator<ElementPosition> destinationIterator;
    private final @Nullable ListCandidatePool<Solution_, Entity_, Value_> candidatePool;

    /**
     * @param destinationInstance null to draw destinations from {@link ListCandidatePool}
     * @param entityInstance the movable entities; null to draw destinations from the destination instance
     */
    public ListChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, boolean crossingNull,
            DefaultUniDatasetInstance<Solution_, Value_> sourceInstance,
            @Nullable UniDatasetInstance<ElementPosition> destinationInstance,
            @Nullable UniDatasetInstance<Entity_> entityInstance) {
        super(sourceInstance.retiringRandomIterator(random));
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.crossingNull = crossingNull;
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        this.destinationInstance = destinationInstance;
        this.destinationIterator = destinationInstance == null ? null : destinationInstance.iterator(random);
        this.candidatePool = entityInstance == null ? null
                : ListCandidatePool.of(solutionView, variableMetaModel, random, entityInstance);
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Value_ sourceValue) {
        var targetPosition = candidatePool != null
                ? candidatePool.drawDestination(sourceValue, crossingNull,
                        destination -> isValidChange(sourceValue, destination, false))
                : drawMatching(Objects.requireNonNull(destinationIterator),
                        Objects.requireNonNull(destinationInstance).size(),
                        destination -> isValidChange(sourceValue, destination, true));
        return targetPosition == null ? null : buildMove(sourceValue, targetPosition);
    }

    /**
     * @param checkRange false if the target entity is known to accept the value
     */
    private boolean isValidChange(Value_ value, ElementPosition targetPosition, boolean checkRange) {
        var currentPosition = solutionView.getPositionOf(variableMetaModel, value);
        if (currentPosition.equals(targetPosition)) { // No change needed; also excludes both-unassigned.
            return false;
        }

        if (currentPosition instanceof PositionInList currentAssigned) {
            if (targetPosition instanceof PositionInList targetAssigned) {
                if (currentAssigned.entity() == targetAssigned.entity()) { // The value is already in the list.
                    var valueCount = solutionView.countValues(variableMetaModel, currentAssigned.entity());
                    if (valueCount == 1) { // The value is the only value in the list; no change.
                        return false;
                    } else {
                        // Either same list, same position (ignore),
                        // or trying to move the value past the end of the list.
                        return targetAssigned.index() != valueCount;
                    }
                }
                // We can move freely between entities, assuming the target entity accepts the value.
                return !checkRange || solutionView.isValueInRange(variableMetaModel, targetAssigned.entity(), value);
            } else { // Unassigning an assigned value never violates a value range.
                return true;
            }
        } else { // Assigning a currently unassigned value; targetPosition is a PositionInList (checked above).
            var targetAssigned = (PositionInList) targetPosition;
            return !checkRange || solutionView.isValueInRange(variableMetaModel, targetAssigned.entity(), value);
        }
    }

    private Move<Solution_> buildMove(Value_ sourceValue, ElementPosition targetPosition) {
        var currentPosition = solutionView.getPositionOf(variableMetaModel, sourceValue);
        if (currentPosition instanceof PositionInList currentAssigned) {
            if (targetPosition instanceof PositionInList targetAssigned) {
                return Moves.change(variableMetaModel, currentAssigned, targetAssigned);
            } else { // Assigned value moving to the unassigned pool.
                return Moves.unassign(variableMetaModel, currentAssigned);
            }
        } else { // Unassigned value moving into the list; isValidChange already excluded both-unassigned.
            return Moves.assign(variableMetaModel, sourceValue, (PositionInList) targetPosition);
        }
    }

}
