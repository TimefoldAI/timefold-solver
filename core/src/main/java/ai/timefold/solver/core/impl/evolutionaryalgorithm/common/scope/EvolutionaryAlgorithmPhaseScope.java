package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class EvolutionaryAlgorithmPhaseScope<Solution_> extends AbstractPhaseScope<Solution_> {

    private EvolutionaryAlgorithmStepScope<Solution_> lastCompletedStepScope;
    private Population<Solution_, ?> population;

    public EvolutionaryAlgorithmPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        super(solverScope, phaseIndex);
        this.lastCompletedStepScope = new EvolutionaryAlgorithmStepScope<>(this, 0, null);
    }

    public void setLastCompletedStepScope(EvolutionaryAlgorithmStepScope<Solution_> lastCompletedStepScope) {
        this.lastCompletedStepScope = lastCompletedStepScope;
    }

    @Override
    public AbstractStepScope<Solution_> getLastCompletedStepScope() {
        return lastCompletedStepScope;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> Population<Solution_, Score_> getPopulation() {
        return (Population<Solution_, Score_>) population;
    }

    public void setPopulation(Population<Solution_, ?> population) {
        this.population = population;
    }

    public EvolutionaryAlgorithmPhaseScope<Solution_> copy(InnerScoreDirector<Solution_, ?> scoreDirector) {
        var solverScopeCopy = getSolverScope().copy(scoreDirector);
        var copy = new EvolutionaryAlgorithmPhaseScope<>(solverScopeCopy, phaseIndex);
        copy.startingSystemTimeMillis = startingSystemTimeMillis;
        copy.startingScoreCalculationCount = startingScoreCalculationCount;
        copy.startingMoveEvaluationCount = startingMoveEvaluationCount;
        copy.startingScore = startingScore;
        copy.setTermination(getTermination());
        copy.lastCompletedStepScope = lastCompletedStepScope;
        copy.population = population;
        return copy;
    }

}
