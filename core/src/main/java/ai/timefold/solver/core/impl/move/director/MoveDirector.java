package ai.timefold.solver.core.impl.move.director;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.MoveAdapters;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class MoveDirector<Solution_, Score_ extends Score<Score_>>
        extends DefaultMutableSolutionView<Solution_>
        implements InnerMutableSolutionView<Solution_>, Rebaser
        permits EphemeralMoveDirector {

    private final InnerScoreDirector<Solution_, Score_> backingScoreDirector;

    public MoveDirector(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        super(scoreDirector);
        this.backingScoreDirector = Objects.requireNonNull(scoreDirector);
    }

    protected MoveDirector(
            VariableDescriptorAwareScoreDirector<Solution_> externalScoreDirector,
            InnerScoreDirector<Solution_, Score_> backingScoreDirector) {
        super(externalScoreDirector);
        this.backingScoreDirector = Objects.requireNonNull(backingScoreDirector);
    }

    /**
     * Execute a given move and make sure shadow variables are up to date after that.
     */
    public final void execute(Move<Solution_> move) {
        move.execute(this);
        externalScoreDirector.triggerVariableListeners();
    }

    // Only used in tests of legacy moves.
    public final void execute(ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move) {
        execute(MoveAdapters.toNewMove(move));
    }

    public final InnerScore<Score_> executeTemporary(Move<Solution_> move) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move);
        var score = backingScoreDirector.calculateScore();
        ephemeralMoveDirector.close(); // This undoes the move.
        return score;
    }

    public <Result_> Result_ executeTemporary(Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, Result_> postprocessor) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move);
        var score = backingScoreDirector.calculateScore();
        var result = postprocessor.apply(score, ephemeralMoveDirector.createUndoMove());
        ephemeralMoveDirector.close(); // This undoes the move.
        return result;
    }

    // Only used in tests of legacy moves.
    public final <Result_> Result_ executeTemporary(ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, Result_> postprocessor) {
        return executeTemporary(MoveAdapters.toNewMove(move), postprocessor);
    }

    @Override
    public final <T> @Nullable T rebase(@Nullable T problemFactOrPlanningEntity) {
        return externalScoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    /**
     * Moves that are to be undone later need to be run with the instance returned by this method.
     * To undo the move, remember to call {@link EphemeralMoveDirector#close()}.
     *
     * @return never null
     */
    final EphemeralMoveDirector<Solution_, Score_> ephemeral() {
        return new EphemeralMoveDirector<>(backingScoreDirector);
    }

    @Override
    public final VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return externalScoreDirector;
    }

    /**
     * Allows for reading data produced by a temporary move, before it is undone.
     * The score argument represents the score after executing the move on the solution.
     * The move argument represents the undo move for that move.
     * 
     * @param <Solution_> type of the solution
     * @param <Score_> score of the move
     * @param <Result_> user-defined return type of the function
     */
    @FunctionalInterface
    public interface TemporaryMovePostprocessor<Solution_, Score_ extends Score<Score_>, Result_>
            extends BiFunction<InnerScore<Score_>, Move<Solution_>, Result_> {

    }

}
