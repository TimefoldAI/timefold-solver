package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

public final class EvolutionaryAlgorithmStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope;
    private Individual<Solution_, ?> stepIndividual;

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex(), null);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope,
            Individual<Solution_, ?> stepIndividual) {
        this(phaseScope, phaseScope.getNextStepIndex(), stepIndividual);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, int stepIndex,
            Individual<Solution_, ?> stepIndividual) {
        super(stepIndex);
        this.phaseScope = phaseScope;
        this.stepIndividual = stepIndividual;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> Individual<Solution_, Score_> getStepIndividual() {
        return (Individual<Solution_, Score_>) stepIndividual;
    }

    public void setStepIndividual(Individual<Solution_, ?> stepIndividual) {
        this.stepIndividual = stepIndividual;
    }

    @Override
    public EvolutionaryAlgorithmPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    @Override
    public Solution_ cloneWorkingSolution() {
        return getScoreDirector().cloneSolution(stepIndividual.getSolution());
    }

}
