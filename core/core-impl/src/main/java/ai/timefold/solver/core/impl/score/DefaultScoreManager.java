package ai.timefold.solver.core.impl.score;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @deprecated Use {@link DefaultSolutionManager} instead.
 */
@Deprecated(forRemoval = true)
public final class DefaultScoreManager<Solution_, Score_ extends Score<Score_>>
        implements ScoreManager<Solution_, Score_> {

    private final SolutionManager<Solution_, Score_> solutionManager;

    public DefaultScoreManager(SolutionManager<Solution_, Score_> solutionManager) {
        this.solutionManager = Objects.requireNonNull(solutionManager);
    }

    @Override
    public Score_ updateScore(Solution_ solution) {
        return solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
    }

    @Override
    public String getSummary(Solution_ solution) {
        return explainScore(solution)
                .getSummary();
    }

    @Override
    public ScoreExplanation<Solution_, Score_> explainScore(Solution_ solution) {
        return solutionManager.explain(solution, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
    }

    @Override
    public Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        return solutionManager.update(solution, solutionUpdatePolicy);
    }

    @Override
    public ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        return solutionManager.explain(solution, solutionUpdatePolicy);
    }

}
