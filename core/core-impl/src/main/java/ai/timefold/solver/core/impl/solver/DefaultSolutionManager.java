package ai.timefold.solver.core.impl.solver;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
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

    private <Result_> Result_ callScoreDirector(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy,
            Function<InnerScoreDirector<Solution_, Score_>, Result_> function, boolean enableConstraintMatch) {
        var isShadowVariableUpdateEnabled = solutionUpdatePolicy.isShadowVariableUpdateEnabled();
        var nonNullSolution = Objects.requireNonNull(solution);
        try (var scoreDirector =
                scoreDirectorFactory.buildScoreDirector(false, enableConstraintMatch, !isShadowVariableUpdateEnabled)) {
            scoreDirector.setWorkingSolution(nonNullSolution); // Init the ScoreDirector first, else NPEs may be thrown.
            if (enableConstraintMatch && !scoreDirector.isConstraintMatchEnabled()) {
                throw new IllegalStateException("When constraintMatchEnabled is disabled, this method should not be called.");
            }
            if (isShadowVariableUpdateEnabled) {
                scoreDirector.forceTriggerVariableListeners();
            }
            if (solutionUpdatePolicy.isScoreUpdateEnabled()) {
                scoreDirector.calculateScore();
            }
            return function.apply(scoreDirector);
        }
    }

    @Override
    public ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        var currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        var explanation = callScoreDirector(solution, solutionUpdatePolicy, DefaultScoreExplanation::new, true);
        assertFreshScore(solution, currentScore, explanation.getScore(), solutionUpdatePolicy);
        return explanation;
    }

    private void assertFreshScore(Solution_ solution, Score_ currentScore, Score_ calculatedScore,
            SolutionUpdatePolicy solutionUpdatePolicy) {
        if (!solutionUpdatePolicy.isScoreUpdateEnabled() && currentScore != null) {
            // Score update is not enabled and score is not null; this means the score is supposed to be valid.
            // Yet it is different from a freshly calculated score, suggesting previous score corruption.
            if (!calculatedScore.equals(currentScore)) {
                throw new IllegalStateException("""
                        Current score (%s) and freshly calculated score (%s) for solution (%s) do not match.
                        Maybe run %s environment mode to check for score corruptions.
                        Otherwise enable %s.%s to update the stale score.
                        """
                        .formatted(currentScore, calculatedScore, solution, EnvironmentMode.FULL_ASSERT,
                                SolutionUpdatePolicy.class.getSimpleName(),
                                SolutionUpdatePolicy.UPDATE_ALL));
            }
        }
    }

    @Override
    public ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy fetchPolicy,
            SolutionUpdatePolicy solutionUpdatePolicy) {
        Objects.requireNonNull(fetchPolicy, "fetchPolicy");
        var currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        var analysis = callScoreDirector(solution, solutionUpdatePolicy,
                scoreDirector -> scoreDirector.buildScoreAnalysis(fetchPolicy == ScoreAnalysisFetchPolicy.FETCH_ALL), true);
        assertFreshScore(solution, currentScore, analysis.score(), solutionUpdatePolicy);
        return analysis;
    }

}
