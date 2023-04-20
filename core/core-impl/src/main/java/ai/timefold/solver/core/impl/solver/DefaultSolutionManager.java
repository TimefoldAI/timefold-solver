package ai.timefold.solver.core.impl.solver;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class DefaultSolutionManager<Solution_, Score_ extends Score<Score_>>
        implements SolutionManager<Solution_, Score_> {

    private final InnerScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;

    public <ProblemId_> DefaultSolutionManager(SolverManager<Solution_, ProblemId_> solverManager) {
        this(((DefaultSolverManager<Solution_, ProblemId_>) solverManager).getSolverFactory());
    }

    public DefaultSolutionManager(SolverFactory<Solution_> solverFactory) {
        this.scoreDirectorFactory = ((DefaultSolverFactory<Solution_>) solverFactory).getScoreDirectorFactory();
    }

    public InnerScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return scoreDirectorFactory;
    }

    @Override
    public Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        if (solutionUpdatePolicy == SolutionUpdatePolicy.NO_UPDATE) {
            throw new IllegalArgumentException("Can not call " + this.getClass().getSimpleName()
                    + ".update() with this solutionUpdatePolicy (" + solutionUpdatePolicy + ").");
        }
        return callScoreDirector(solution, solutionUpdatePolicy,
                s -> (Score_) s.getSolutionDescriptor().getScore(s.getWorkingSolution()),
                false);
    }

    @Override
    public ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        Score_ currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        ScoreExplanation<Solution_, Score_> explanation =
                callScoreDirector(solution, solutionUpdatePolicy, DefaultScoreExplanation::new, true);
        if (!solutionUpdatePolicy.isScoreUpdateEnabled() && currentScore != null) {
            // Score update is not enabled and score is not null; this means the score is supposed to be valid.
            // Yet it is different from a freshly calculated score, suggesting previous score corruption.
            Score_ freshScore = explanation.getScore();
            if (!freshScore.equals(currentScore)) {
                throw new IllegalStateException("Current score (" + currentScore + ") and freshly calculated score ("
                        + freshScore + ") for solution (" + solution + ") do not match.\n"
                        + "Maybe run " + EnvironmentMode.FULL_ASSERT + " to check for score corruptions.\n"
                        + "Otherwise enable " + SolutionUpdatePolicy.class.getSimpleName()
                        + "." + SolutionUpdatePolicy.UPDATE_ALL + " to update the stale score.");
            }
        }
        return explanation;
    }

    private <Result_> Result_ callScoreDirector(Solution_ solution,
            SolutionUpdatePolicy solutionUpdatePolicy, Function<InnerScoreDirector<Solution_, Score_>, Result_> function,
            boolean enableConstraintMatch) {
        Solution_ nonNullSolution = Objects.requireNonNull(solution);
        try (InnerScoreDirector<Solution_, Score_> scoreDirector =
                scoreDirectorFactory.buildScoreDirector(false, enableConstraintMatch)) {
            scoreDirector.setWorkingSolution(nonNullSolution); // Init the ScoreDirector first, else NPEs may be thrown.
            if (enableConstraintMatch && !scoreDirector.isConstraintMatchEnabled()) {
                throw new IllegalStateException("When constraintMatchEnabled is disabled, this method should not be called.");
            }
            if (solutionUpdatePolicy.isShadowVariableUpdateEnabled()) {
                scoreDirector.forceTriggerVariableListeners();
            }
            if (solutionUpdatePolicy.isScoreUpdateEnabled()) {
                scoreDirector.calculateScore();
            }
            return function.apply(scoreDirector);
        }
    }
}
