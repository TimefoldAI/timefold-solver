package ai.timefold.solver.core.impl.move;

import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for executing moves on a bound planning solution instance.
 * <p>
 * Created via {@link MoveRunner#using(Object)}, this context binds a specific solution
 * instance to the runner and exposes execution methods.
 * <p>
 * This class is NOT thread-safe.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public final class DefaultMoveRunContext<Solution_> implements MoveRunContext<Solution_> {

    private final AbstractScoreDirector<Solution_, ?, ?> scoreDirector;

    DefaultMoveRunContext(AbstractScoreDirector<Solution_, ?, ?> scoreDirector) {
        this.scoreDirector = Objects.requireNonNull(scoreDirector, "scoreDirector");
    }

    @Override
    public void execute(Move<Solution_> move) {
        scoreDirector.executeMove(Objects.requireNonNull(move, "move"));
    }

    @Override
    public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler) {
        Objects.requireNonNull(exceptionHandler, "exceptionHandler");
        try {
            scoreDirector.executeMove(Objects.requireNonNull(move, "move"));
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }

    @Override
    public void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> assertions) {
        scoreDirector.executeTemporaryMove(
                Objects.requireNonNull(move, "move"),
                Objects.requireNonNull(assertions, "assertions"));
    }

}
