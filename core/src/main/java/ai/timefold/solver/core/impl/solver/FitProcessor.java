package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class FitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final ScoreAnalysis<Score_> originalScoreAnalysis;
    private final ScoreAnalysisFetchPolicy fetchPolicy;
    private final Function<In_, Out_> valueResultFunction;
    private final In_ clonedElement;

    public FitProcessor(DefaultSolverFactory<Solution_> solverFactory, Function<In_, Out_> valueResultFunction,
            ScoreAnalysis<Score_> originalScoreAnalysis, In_ clonedElement, ScoreAnalysisFetchPolicy fetchPolicy) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.originalScoreAnalysis = Objects.requireNonNull(originalScoreAnalysis);
        this.fetchPolicy = Objects.requireNonNull(fetchPolicy);
        this.valueResultFunction = valueResultFunction;
        this.clonedElement = clonedElement;
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        // The placers needs to be filtered.
        // If anything else than the cloned element is unassigned, we want to keep it unassigned.
        // Otherwise the solution would have to explicitly pin everything other than the cloned element.
        var entityPlacer = buildEntityPlacer()
                .rebuildWithFilter((solution, selection) -> selection == clonedElement);

        var solverScope = new SolverScope<Solution_>();
        solverScope.setWorkingRandom(new Random(0)); // We will evaluate every option; random does not matter.
        solverScope.setScoreDirector(scoreDirector);
        var phaseScope = new ConstructionHeuristicPhaseScope<>(solverScope, -1);
        var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);
        entityPlacer.solvingStarted(solverScope);
        entityPlacer.phaseStarted(phaseScope);
        entityPlacer.stepStarted(stepScope);

        try (scoreDirector) {
            var placementIterator = entityPlacer.iterator();
            if (!placementIterator.hasNext()) {
                throw new IllegalStateException("""
                        Impossible state: entity placer (%s) has no placements.
                        """.formatted(entityPlacer));
            }
            var placement = placementIterator.next();
            var recommendedFitList = new ArrayList<RecommendedFit<Out_, Score_>>();
            var moveIndex = 0L;
            for (var move : placement) {
                recommendedFitList.add(execute(scoreDirector, move, moveIndex, clonedElement, valueResultFunction));
                moveIndex++;
            }
            recommendedFitList.sort(null);
            return recommendedFitList;
        } finally {
            entityPlacer.stepEnded(stepScope);
            entityPlacer.phaseEnded(phaseScope);
            entityPlacer.solvingEnded(solverScope);
        }
    }

    private EntityPlacer<Solution_> buildEntityPlacer() {
        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        var phaseList = solver.getPhaseList();
        long constructionHeuristicCount = phaseList.stream()
                .filter(s -> (s instanceof DefaultConstructionHeuristicPhase))
                .count();
        if (constructionHeuristicCount != 1) {
            throw new IllegalStateException(
                    "Fit Recommendation API requires the solver config to have exactly one construction heuristic phase, but it has (%s) instead."
                            .formatted(constructionHeuristicCount));
        }
        var phase = phaseList.get(0);
        if (phase instanceof DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase) {
            return constructionHeuristicPhase.getEntityPlacer();
        } else {
            throw new IllegalStateException(
                    "Fit Recommendation API requires the first solver phase (%s) in the solver config to be a construction heuristic."
                            .formatted(phase));
        }
    }

    private RecommendedFit<Out_, Score_> execute(InnerScoreDirector<Solution_, Score_> scoreDirector, Move<Solution_> move,
            long moveIndex, In_ clonedElement, Function<In_, Out_> propositionFunction) {
        var undo = move.doMove(scoreDirector);
        var newScoreAnalysis = scoreDirector.buildScoreAnalysis(fetchPolicy == ScoreAnalysisFetchPolicy.FETCH_ALL);
        var newScoreDifference = newScoreAnalysis.diff(originalScoreAnalysis);
        var result = propositionFunction.apply(clonedElement);
        var recommendation = new DefaultRecommendedFit<>(moveIndex, result, newScoreDifference);
        undo.doMoveOnly(scoreDirector);
        return recommendation;
    }

}
