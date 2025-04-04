package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class Assigner<Solution_, Score_ extends Score<Score_>, Recommendation_, In_, Out_>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<Recommendation_>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final Function<In_, Out_> propositionFunction;
    private final RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor;
    private final ScoreAnalysisFetchPolicy fetchPolicy;
    private final Solution_ originalSolution;
    private final In_ originalElement;

    public Assigner(DefaultSolverFactory<Solution_> solverFactory, Function<In_, Out_> propositionFunction,
            RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor,
            ScoreAnalysisFetchPolicy fetchPolicy, Solution_ originalSolution, In_ originalElement) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.propositionFunction = Objects.requireNonNull(propositionFunction);
        this.recommendationConstructor = Objects.requireNonNull(recommendationConstructor);
        this.fetchPolicy = Objects.requireNonNull(fetchPolicy);
        this.originalSolution = Objects.requireNonNull(originalSolution);
        this.originalElement = Objects.requireNonNull(originalElement);
    }

    @Override
    public List<Recommendation_> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var initializationStatistics = solutionDescriptor.computeInitializationStatistics(originalSolution);
        var uninitializedCount =
                initializationStatistics.uninitializedEntityCount() + initializationStatistics.unassignedValueCount();
        if (uninitializedCount > 1) {
            throw new IllegalStateException("""
                    Solution (%s) has (%d) uninitialized elements.
                    Assignment Recommendation API requires at most one uninitialized element in the solution."""
                    .formatted(originalSolution, uninitializedCount));
        }
        var originalScoreAnalysis = scoreDirector.buildScoreAnalysis(fetchPolicy);
        var clonedElement = scoreDirector.lookUpWorkingObject(originalElement);
        var processor = new AssignmentProcessor<>(solverFactory, propositionFunction, recommendationConstructor, fetchPolicy,
                clonedElement, originalScoreAnalysis);
        return processor.apply(scoreDirector);
    }

}
