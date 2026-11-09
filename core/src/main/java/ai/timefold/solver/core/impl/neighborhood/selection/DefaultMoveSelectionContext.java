package ai.timefold.solver.core.impl.neighborhood.selection;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.selection.PhaseContext;
import ai.timefold.solver.core.preview.api.neighborhood.selection.StepContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One mutable instance per context kind per phase run, so that strategy calls do not allocate.
 */
@NullMarked
public final class DefaultMoveSelectionContext<Solution_> implements PhaseContext<Solution_>, StepContext<Solution_> {

    private final List<MoveSource<Solution_>> moveSourceList;
    private @Nullable Solution_ solution = null;

    public DefaultMoveSelectionContext(List<MoveSource<Solution_>> moveSourceList) {
        this.moveSourceList = moveSourceList;
    }

    public DefaultMoveSelectionContext<Solution_> withSolution(Solution_ solution) {
        this.solution = solution;
        return this;
    }

    @Override
    public List<MoveSource<Solution_>> moveSources() {
        return moveSourceList;
    }

    @Override
    public Solution_ solution() {
        return Objects.requireNonNull(solution);
    }

}
