package ai.timefold.solver.core.impl.move.director;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

final class RecordedUndoMove<Solution_> implements Move<Solution_> {

    private final List<ChangeAction<Solution_>> variableChangeActionList;

    RecordedUndoMove(List<ChangeAction<Solution_>> variableChangeActionList) {
        this.variableChangeActionList = Objects.requireNonNull(variableChangeActionList);
    }

    @Override
    public void execute(@NonNull MutableSolutionView<Solution_> solutionView) {
        var scoreDirector = ((InnerMutableSolutionView<Solution_>) solutionView).getScoreDirector();
        for (var changeAction : variableChangeActionList) {
            changeAction.undo(scoreDirector);
        }
    }

    @Override
    public @NonNull Move<Solution_> rebase(@NonNull Rebaser rebaser) {
        return new RecordedUndoMove<>(variableChangeActionList.stream()
                .map(changeAction -> changeAction.rebase(rebaser))
                .toList());
    }

    List<ChangeAction<Solution_>> getVariableChangeActionList() {
        return variableChangeActionList;
    }
}