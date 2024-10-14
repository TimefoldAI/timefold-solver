package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionState;

final class RecordedUndoMove<Solution_> implements Move<Solution_> {

    private final List<ChangeAction<Solution_>> variableChangeActionList;

    RecordedUndoMove(List<ChangeAction<Solution_>> variableChangeActionList) {
        this.variableChangeActionList = Objects.requireNonNull(variableChangeActionList);
    }

    @Override
    public void run(MutableSolutionState<Solution_> mutableSolutionState) {
        var scoreDirector = ((InnerMutableSolutionState<Solution_>) mutableSolutionState).getScoreDirector();
        for (var changeAction : variableChangeActionList) {
            changeAction.undo(scoreDirector);
        }
    }

    @Override
    public Move<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return new RecordedUndoMove<>(variableChangeActionList.stream()
                .map(changeAction -> changeAction.rebase(solutionState))
                .toList());
    }

}