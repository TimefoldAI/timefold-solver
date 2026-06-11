package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractHybridGeneticSearchDecider<Solution_, Score_ extends Score<Score_>>
        implements EvolutionaryDecider<Solution_, Score_> {

    protected final String logIndentation;
    protected final int populationSize;
    protected final int generationSize;
    protected final int eliteSolutionSize;
    protected final int populationRestartCount;
    protected final BestSolutionRecaller<Solution_> bestSolutionRecaller;

    private long lastBestIter;

    protected AbstractHybridGeneticSearchDecider(String logIndentation, int populationSize, int generationSize,
            int eliteSolutionSize, int populationRestartCount, BestSolutionRecaller<Solution_> bestSolutionRecaller) {
        this.logIndentation = logIndentation;
        this.populationSize = populationSize;
        this.generationSize = generationSize;
        this.eliteSolutionSize = eliteSolutionSize;
        this.populationRestartCount = populationRestartCount;
        this.bestSolutionRecaller = bestSolutionRecaller;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public abstract void restart(EvolutionaryAlgorithmStepScope<Solution_> stepScope, int size);

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        this.lastBestIter = 0;
    }

    @Override
    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        if (lastBestIter == 0) {
            this.lastBestIter = phaseScope.getPopulation().getStatistics().individualCount();
        } else {
            var size = populationSize - eliteSolutionSize;
            var restart =
                    (phaseScope.getPopulation().getStatistics().individualCount() - lastBestIter) > populationRestartCount;
            if (restart) {
                restart(stepScope, size);
                this.lastBestIter = phaseScope.getPopulation().getStatistics().individualCount();
            }
        }
    }

    @Override
    public void stepEnded(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        // Do nothing
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // Overridable by a subclass.
    }

    @NullMarked
    @SuppressWarnings("rawtypes")
    public abstract static class AbstractBuilder<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>> {

        public String logIndentation;
        public int populationSize;
        public int generationSize;
        public int eliteSolutionSize;
        public int populationRestartCount;
        @Nullable
        public HybridGeneticSearchWorkerContext<Solution_, Score_, State_> context;
        @Nullable
        public PhaseTermination<Solution_> phaseTermination;
        @Nullable
        public BestSolutionRecaller<Solution_> bestSolutionRecaller;

        public AbstractBuilder<Solution_, Score_, State_> withLogIndentation(String logIndentation) {
            this.logIndentation = logIndentation;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_> withPopulationSize(int populationSize) {
            this.populationSize = populationSize;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_> withGenerationSize(int generationSize) {
            this.generationSize = generationSize;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_> withEliteSolutionSize(int eliteSolutionSize) {
            this.eliteSolutionSize = eliteSolutionSize;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_> withPopulationRestartCount(int populationRestartCount) {
            this.populationRestartCount = populationRestartCount;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_>
                withContext(HybridGeneticSearchWorkerContext<Solution_, Score_, State_> workerContext) {
            this.context = workerContext;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_>
                withPhaseTermination(PhaseTermination<Solution_> phaseTermination) {
            this.phaseTermination = phaseTermination;
            return this;
        }

        public AbstractBuilder<Solution_, Score_, State_>
                withBestSolutionRecaller(BestSolutionRecaller<Solution_> bestSolutionRecaller) {
            this.bestSolutionRecaller = bestSolutionRecaller;
            return this;
        }

        public abstract <Type_ extends EvolutionaryDecider> Type_ build();
    }
}
