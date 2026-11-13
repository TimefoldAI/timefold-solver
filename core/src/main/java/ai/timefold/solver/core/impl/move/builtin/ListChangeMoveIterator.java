package ai.timefold.solver.core.impl.move.builtin;

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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListChangeMoveIterator<Solution_, Entity_, Value_>
        extends AbstractRepeatedDrawMoveIterator<Solution_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final SolutionView<Solution_> solutionView;
    private final ListCandidateSource<Entity_, Value_> candidateSource;

    /**
     * @param sourceInstance the values to move
     * @param candidateSource the destinations; it decides whether the unassigned destination is one of them
     */
    public ListChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            DefaultUniDatasetInstance<Solution_, Value_> sourceInstance,
            ListCandidateSource<Entity_, Value_> candidateSource) {
        super(sourceInstance.retiringRandomIterator(random));
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.solutionView = session.getSolutionView();
        this.candidateSource = Objects.requireNonNull(candidateSource);
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Value_ sourceValue) {
        var targetPosition = candidateSource.drawDestination(sourceValue,
                destination -> isValidChange(sourceValue, destination));
        return targetPosition == null ? null : buildMove(sourceValue, targetPosition);
    }

    /**
     * The candidate source only gives destinations in entities which accept the value.
     */
    private boolean isValidChange(Value_ value, ElementPosition targetPosition) {
        var currentPosition = solutionView.getPositionOf(variableMetaModel, value);
        if (currentPosition.equals(targetPosition)) { // No change needed; also excludes both-unassigned.
            return false;
        }

        if (currentPosition instanceof PositionInList currentAssigned
                && targetPosition instanceof PositionInList targetAssigned
                && currentAssigned.entity() == targetAssigned.entity()) {
            var valueCount = solutionView.countValues(variableMetaModel, currentAssigned.entity());
            // The index applies after the value leaves its list, so the own end slot is out of range;
            // index valueCount - 1 already moves the value to the end, and a single value cannot move.
            return valueCount > 1 && targetAssigned.index() != valueCount;
        }
        // Another entity, unassigning or assigning; the candidate source only gives entities which accept the value.
        return true;
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
