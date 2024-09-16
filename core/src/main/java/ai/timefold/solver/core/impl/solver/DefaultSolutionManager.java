package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedFit;
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

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final InnerScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;

    public <ProblemId_> DefaultSolutionManager(SolverManager<Solution_, ProblemId_> solverManager) {
        this(((DefaultSolverManager<Solution_, ProblemId_>) solverManager).getSolverFactory());
    }

    public DefaultSolutionManager(SolverFactory<Solution_> solverFactory) {
        this.solverFactory = ((DefaultSolverFactory<Solution_>) solverFactory);
        this.scoreDirectorFactory = this.solverFactory.getScoreDirectorFactory();
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
                s -> s.getSolutionDescriptor().getScore(s.getWorkingSolution()), false, false);
    }

    private <Result_> Result_ callScoreDirector(Solution_ solution,
            SolutionUpdatePolicy solutionUpdatePolicy, Function<InnerScoreDirector<Solution_, Score_>, Result_> function,
            boolean enableConstraintMatch, boolean cloneSolution) {
        var isShadowVariableUpdateEnabled = solutionUpdatePolicy.isShadowVariableUpdateEnabled();
        var nonNullSolution = Objects.requireNonNull(solution);
        try (var scoreDirector = getScoreDirectorFactory().buildScoreDirector(cloneSolution, enableConstraintMatch,
                !isShadowVariableUpdateEnabled)) {
            nonNullSolution = cloneSolution ? scoreDirector.cloneSolution(nonNullSolution) : nonNullSolution;
            scoreDirector.setWorkingSolution(nonNullSolution);
            if (enableConstraintMatch && !scoreDirector.isConstraintMatchEnabled()) {
                throw new IllegalStateException("""
                        Requested constraint matching but score director doesn't support it.
                        Maybe use Constraint Streams instead of Easy or Incremental score calculator?""");
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
        var explanation = callScoreDirector(solution, solutionUpdatePolicy, DefaultScoreExplanation::new, true, false);
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
                        .formatted(currentScore, calculatedScore, solution, EnvironmentMode.TRACKED_FULL_ASSERT,
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
                scoreDirector -> scoreDirector.buildScoreAnalysis(fetchPolicy == ScoreAnalysisFetchPolicy.FETCH_ALL), true,
                false);
        assertFreshScore(solution, currentScore, analysis.score(), solutionUpdatePolicy);
        return analysis;
    }

    @Override
    public <In_, Out_> List<RecommendedFit<Out_, Score_>> recommendFit(Solution_ solution, In_ fittedEntityOrElement,
            Function<In_, Out_> propositionFunction, ScoreAnalysisFetchPolicy fetchPolicy) {
        var fitter = new Fitter<Solution_, In_, Out_, Score_>(solverFactory, solution, fittedEntityOrElement,
                propositionFunction, fetchPolicy);
        return callScoreDirector(solution, SolutionUpdatePolicy.UPDATE_ALL, fitter, true, true);
    }

}
