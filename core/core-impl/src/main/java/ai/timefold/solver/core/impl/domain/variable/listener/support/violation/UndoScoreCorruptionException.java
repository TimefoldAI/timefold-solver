package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

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

    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getBeforeMoveSolution() {
        return (Solution_) beforeMoveSolution;
    }

    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterMoveSolution() {
        return (Solution_) afterMoveSolution;
    }

    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterUndoSolution() {
        return (Solution_) afterUndoSolution;
    }

    @SuppressWarnings("unchecked")
    public <Solution_> Move<Solution_> getMove() {
        return (Move<Solution_>) move;
    }

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
