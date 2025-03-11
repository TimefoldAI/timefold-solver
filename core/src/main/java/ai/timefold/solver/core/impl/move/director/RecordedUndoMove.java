package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

@NullMarked
record RecordedUndoMove<Solution_>(List<ChangeAction<Solution_>> variableChangeActionList)
        implements
            Move<Solution_> {

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

    @Override
    public String toString() {
        return "Undo(%s)"
                .formatted(variableChangeActionList);
    }
}