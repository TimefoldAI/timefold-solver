package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionView;
import ai.timefold.solver.core.api.move.Rebaser;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;

final class RecordedUndoMove<Solution_> implements Move<Solution_> {

    private final List<ChangeAction<Solution_>> variableChangeActionList;

    RecordedUndoMove(List<ChangeAction<Solution_>> variableChangeActionList) {
        this.variableChangeActionList = Objects.requireNonNull(variableChangeActionList);
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        var scoreDirector = ((InnerMutableSolutionView<Solution_>) solutionView).getScoreDirector();
        for (var changeAction : variableChangeActionList) {
            changeAction.undo(scoreDirector);
        }
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return new RecordedUndoMove<>(variableChangeActionList.stream()
                .map(changeAction -> changeAction.rebase(rebaser))
                .toList());
    }

}