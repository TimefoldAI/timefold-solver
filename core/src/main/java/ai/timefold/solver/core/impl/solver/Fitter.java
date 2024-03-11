package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class Fitter<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final Solution_ originalSolution;
    private final In_ originalElement;
    private final Function<In_, Out_> propositionFunction;
    private final ScoreAnalysisFetchPolicy fetchPolicy;

    public Fitter(DefaultSolverFactory<Solution_> solverFactory, Solution_ originalSolution, In_ originalElement,
            Function<In_, Out_> propositionFunction, ScoreAnalysisFetchPolicy fetchPolicy) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.originalSolution = Objects.requireNonNull(originalSolution);
        this.originalElement = Objects.requireNonNull(originalElement);
        this.propositionFunction = Objects.requireNonNull(propositionFunction);
        this.fetchPolicy = Objects.requireNonNull(fetchPolicy);
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var initializationStatistics = solutionDescriptor.computeInitializationStatistics(originalSolution);
        var uninitializedCount =
                initializationStatistics.uninitializedEntityCount() + initializationStatistics.unassignedValueCount();
        if (uninitializedCount > 1) {
            throw new IllegalStateException("""
                    Solution (%s) has (%d) uninitialized elements.
                    Fit Recommendation API requires at most one uninitialized element in the solution."""
                    .formatted(originalSolution, uninitializedCount));
        }
        var originalScoreAnalysis = scoreDirector.buildScoreAnalysis(fetchPolicy == ScoreAnalysisFetchPolicy.FETCH_ALL,
                InnerScoreDirector.ScoreAnalysisMode.RECOMMENDATION_API);
        var clonedElement = scoreDirector.lookUpWorkingObject(originalElement);
        var processor =
                new FitProcessor<>(solverFactory, propositionFunction, originalScoreAnalysis, clonedElement, fetchPolicy);
        return processor.apply(scoreDirector);
    }

}
