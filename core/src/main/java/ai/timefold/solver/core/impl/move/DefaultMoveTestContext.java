package ai.timefold.solver.core.impl.move;

import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveTestContext;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveTestContext<Solution_> implements MoveTestContext<Solution_> {

    private final AbstractScoreDirector<Solution_, ?, ?> scoreDirector;

    DefaultMoveTestContext(AbstractScoreDirector<Solution_, ?, ?> scoreDirector) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector, "scoreDirector");
    }

    @Override
    public void execute(Move<Solution_> move) {
        scoreDirector.executeMove(Objects.requireNonNull(move, "move"));
    }

    @Override
    public void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> callback) {
        scoreDirector.executeTemporaryMove(
                Objects.requireNonNull(move, "move"),
                Objects.requireNonNull(callback, "callback"),
                false);
    }

    public AbstractScoreDirector<Solution_, ?, ?> getScoreDirector() {
        return scoreDirector;
    }

}
