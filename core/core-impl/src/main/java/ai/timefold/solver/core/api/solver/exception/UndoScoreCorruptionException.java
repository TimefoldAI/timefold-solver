package ai.timefold.solver.core.api.solver.exception;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * An exception that is thrown in {@link ai.timefold.solver.core.config.solver.EnvironmentMode#FULL_ASSERT_WITH_TRACKING} when
 * undo score corruption is detected. It contains the working solution before the move, after the move, and after the undo move,
 * as well as the move that caused the corruption. You can catch this exception to create a reproducer of the corruption.
 */
public class UndoScoreCorruptionException extends IllegalStateException {
    private final Object beforeMoveSolution;
    private final Object afterMoveSolution;
    private final Object afterUndoSolution;

    @SuppressWarnings("rawtypes")
    private final Move move;

    @SuppressWarnings("rawtypes")
    private final AbstractScoreDirectorFactory scoreDirectorFactory;

    public UndoScoreCorruptionException(String message, Object beforeMoveSolution, Object afterMoveSolution,
            Object afterUndoSolution,
            @SuppressWarnings("rawtypes") Move move,
            @SuppressWarnings("rawtypes") AbstractScoreDirectorFactory scoreDirectorFactory) {
        super(message);
        this.beforeMoveSolution = beforeMoveSolution;
        this.afterMoveSolution = afterMoveSolution;
        this.afterUndoSolution = afterUndoSolution;
        this.move = move;
        this.scoreDirectorFactory = scoreDirectorFactory;
    }

    /**
     * Return the state of the working solution before a move was executed.
     *
     * @return the state of the working solution before a move was executed.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getBeforeMoveSolution() {
        return (Solution_) beforeMoveSolution;
    }

    /**
     * Return the state of the working solution after a move was executed, but prior to the undo move.
     *
     * @return the state of the working solution after a move was executed, but prior to the undo move.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterMoveSolution() {
        return (Solution_) afterMoveSolution;
    }

    /**
     * Return the state of the working solution after the undo move was executed.
     *
     * @return the state of the working solution after the undo move was executed.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterUndoSolution() {
        return (Solution_) afterUndoSolution;
    }

    /**
     * Returns the move that caused the corruption, rebased on {@link #getBeforeMoveSolution()}.
     *
     * @return the move that caused the corruption, rebased on {@link #getBeforeMoveSolution}.
     * @throws UnsupportedOperationException If the {@link Move} does not support rebasing.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Move<Solution_> getMove() {
        try (InnerScoreDirector<Solution_, ?> scoreDirector =
                (InnerScoreDirector<Solution_, ?>) scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution((Solution_) beforeMoveSolution);
            return move.rebase(scoreDirector);
        }
    }

    /**
     * Calculate the score of a given solution, using the
     * {@link ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory}
     * of the {@link ai.timefold.solver.core.api.solver.Solver} that encountered the corruption.
     *
     * @param solution The solution to be evaluated
     * @return
     * @param <Solution_>
     * @param <Score_>
     */
    @SuppressWarnings("unchecked")
    public <Solution_, Score_ extends Score<Score_>> Score_ evaluateSolution(Solution_ solution) {
        try (InnerScoreDirector<Solution_, Score_> scoreDirector =
                (InnerScoreDirector<Solution_, Score_>) scoreDirectorFactory.buildScoreDirector()) {
            Solution_ solutionClone = scoreDirector.cloneSolution(solution);
            scoreDirector.setWorkingSolution(solutionClone);
            return scoreDirector.calculateScore();
        }
    }
}
