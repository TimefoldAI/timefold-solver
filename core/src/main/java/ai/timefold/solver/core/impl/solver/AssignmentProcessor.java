package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.ChainedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class AssignmentProcessor<Solution_, Score_ extends Score<Score_>, Recommendation_, In_, Out_>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<Recommendation_>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final Function<In_, Out_> valueResultFunction;
    private final RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor;
    private final ScoreAnalysisFetchPolicy fetchPolicy;
    private final ScoreAnalysis<Score_> originalScoreAnalysis;
    private final In_ clonedElement;

    public AssignmentProcessor(DefaultSolverFactory<Solution_> solverFactory, Function<In_, Out_> valueResultFunction,
            RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor,
            ScoreAnalysisFetchPolicy fetchPolicy, In_ clonedElement, ScoreAnalysis<Score_> originalScoreAnalysis) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.valueResultFunction = valueResultFunction;
        this.recommendationConstructor = Objects.requireNonNull(recommendationConstructor);
        this.fetchPolicy = Objects.requireNonNull(fetchPolicy);
        this.originalScoreAnalysis = Objects.requireNonNull(originalScoreAnalysis);
        this.clonedElement = clonedElement;
    }

    @Override
    public List<Recommendation_> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        // The cloned element may already be assigned.
        // If it is, we need to unassign it before we can run the construction heuristic.
        var supplyManager = scoreDirector.getSupplyManager();
        var solutionDescriptor = solverFactory.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor != null) {
            var demand = listVariableDescriptor.getStateDemand();
            var listVariableStateSupply = supplyManager.demand(demand);
            var elementLocation = listVariableStateSupply.getLocationInList(clonedElement);
            if (elementLocation instanceof LocationInList locationInList) { // Unassign the cloned element.
                var entity = locationInList.entity();
                var index = locationInList.index();
                var listUnassignMove = new ListUnassignMove<>(listVariableDescriptor, entity, index);
                listUnassignMove.doMove(scoreDirector);
            }
            supplyManager.cancel(demand);
        } else {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(clonedElement.getClass());
            for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
                var basicVariableDescriptor = (BasicVariableDescriptor<Solution_>) variableDescriptor;
                if (basicVariableDescriptor.getValue(clonedElement) == null) {
                    // The variable is already unassigned.
                    continue;
                }
                // Uninitialize the basic variable.
                if (basicVariableDescriptor.isChained()) {
                    var demand = new SingletonInverseVariableDemand<>(basicVariableDescriptor);
                    var supply = supplyManager.demand(demand);
                    var move = new ChainedChangeMove<>(basicVariableDescriptor, clonedElement, null, supply);
                    move.doMove(scoreDirector);
                    supplyManager.cancel(demand);
                } else {
                    var move = new ChangeMove<>(basicVariableDescriptor, clonedElement, null);
                    move.doMove(scoreDirector);
                }
            }
        }
        scoreDirector.triggerVariableListeners();

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
            var recommendedAssignmentList = new ArrayList<Recommendation_>();
            var moveIndex = 0L;
            for (var move : placement) {
                recommendedAssignmentList.add(execute(scoreDirector, move, moveIndex, clonedElement, valueResultFunction));
                moveIndex++;
            }
            recommendedAssignmentList.sort(null);
            return recommendedAssignmentList;
        } finally {
            entityPlacer.stepEnded(stepScope);
            entityPlacer.phaseEnded(phaseScope);
            entityPlacer.solvingEnded(solverScope);
        }
    }

    private EntityPlacer<Solution_> buildEntityPlacer() {
        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        var phaseList = solver.getPhaseList();
        var constructionHeuristicCount = phaseList.stream()
                .filter(DefaultConstructionHeuristicPhase.class::isInstance)
                .count();
        if (constructionHeuristicCount != 1) {
            throw new IllegalStateException(
                    "Assignment Recommendation API requires the solver config to have exactly one construction heuristic phase, but it has (%s) instead."
                            .formatted(constructionHeuristicCount));
        }
        var phase = phaseList.get(0);
        if (phase instanceof DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase) {
            return constructionHeuristicPhase.getEntityPlacer();
        } else {
            throw new IllegalStateException(
                    "Assignment Recommendation API requires the first solver phase (%s) in the solver config to be a construction heuristic."
                            .formatted(phase));
        }
    }

    private Recommendation_ execute(InnerScoreDirector<Solution_, Score_> scoreDirector, Move<Solution_> move, long moveIndex,
            In_ clonedElement, Function<In_, Out_> propositionFunction) {
        var undo = move.doMove(scoreDirector);
        var newScoreAnalysis = scoreDirector.buildScoreAnalysis(fetchPolicy == ScoreAnalysisFetchPolicy.FETCH_ALL);
        var newScoreDifference = newScoreAnalysis.diff(originalScoreAnalysis);
        var result = propositionFunction.apply(clonedElement);
        var recommendation = recommendationConstructor.apply(moveIndex, result, newScoreDifference);
        undo.doMoveOnly(scoreDirector);
        return recommendation;
    }

}
