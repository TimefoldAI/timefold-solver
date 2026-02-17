package ai.timefold.solver.core.impl.move;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;

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
    public Move<Solution_> rebase(Lookup lookup) {
        return new RecordedUndoMove<>(variableChangeActionList.stream()
                .map(changeAction -> changeAction.rebase(lookup))
                .toList());
    }

    @Override
    public String toString() {
        return "Undo(%s)"
                .formatted(variableChangeActionList);
    }
}