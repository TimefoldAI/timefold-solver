package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class EvolutionaryAlgorithmStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope;
    @Nullable
    private Individual<Solution_, ?> stepIndividual;
    @Nullable
    private Individual<Solution_, ?> bestIndividual;

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex(), null);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope,
            @Nullable Individual<Solution_, ?> stepIndividual) {
        this(phaseScope, phaseScope.getNextStepIndex(), stepIndividual);
    }

    public EvolutionaryAlgorithmStepScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, int stepIndex,
            @Nullable Individual<Solution_, ?> stepIndividual) {
        super(stepIndex);
        this.phaseScope = phaseScope;
        this.stepIndividual = stepIndividual;
    }

    @SuppressWarnings("unchecked")
    public @Nullable <Score_ extends Score<Score_>> Individual<Solution_, Score_> getStepIndividual() {
        return (Individual<Solution_, Score_>) stepIndividual;
    }

    public void setStepIndividual(Individual<Solution_, ?> stepIndividual) {
        this.stepIndividual = stepIndividual;
    }

    @SuppressWarnings("unchecked")
    public @Nullable <Score_ extends Score<Score_>> Individual<Solution_, Score_> getBestIndividual() {
        return (Individual<Solution_, Score_>) bestIndividual;
    }

    public void setBestIndividual(@Nullable Individual<Solution_, ?> bestIndividual) {
        this.bestIndividual = bestIndividual;
    }

    public <Score_ extends Score<Score_>> Population<Solution_, Score_> getPopulation() {
        return phaseScope.getPopulation();
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
