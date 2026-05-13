package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import java.util.function.IntFunction;

import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.phase.EvolutionaryAlgorithmPhaseLifecycleListener;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.jspecify.annotations.NullMarked;

public final class DefaultEvolutionaryAlgorithmPhase<Solution_> extends AbstractPhase<Solution_>
        implements EvolutionaryAlgorithmPhase<Solution_>, EvolutionaryAlgorithmPhaseLifecycleListener<Solution_> {

    private final EvolutionaryDecider<Solution_, ?> evolutionaryDecider;
    private final boolean isComplex;

    public DefaultEvolutionaryAlgorithmPhase(Builder<Solution_> builder) {
        super(builder);
        this.evolutionaryDecider = builder.evolutionaryDecider;
        this.isComplex = builder.isComplex;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public PhaseType getPhaseType() {
        return PhaseType.EVOLUTIONARY_ALGORITHM;
    }

    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return EventProducerId::evolutionaryAlgorithm;
    }

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = new EvolutionaryAlgorithmPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);
        // Generate individuals and load the initial population
        evolutionaryDecider.loadPopulation(phaseScope);
        while (!phaseTermination.isPhaseTerminated(phaseScope)) {
            var stepScope = new EvolutionaryAlgorithmStepScope<>(phaseScope);
            stepStarted(stepScope);
            // Evolve the current population using the related evolutionary strategy.
            // All logic related to executing operators,
            // individual selection, post-optimization, and so on is handled in this step.
            evolutionaryDecider.evolvePopulation(stepScope);
            stepEnded(stepScope);
        }
        phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        evolutionaryDecider.solvingStarted(solverScope);
        solverScope.startingNow();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        evolutionaryDecider.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        // Initialize the population to allow other operations to execute from a fresh instance
        phaseScope.setPopulation(evolutionaryDecider.emptyPopulation(phaseScope));
        evolutionaryDecider.phaseStarted(phaseScope);
        phaseScope.reset();
    }

    @Override
    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        evolutionaryDecider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        var statistics = phaseScope.getPopulation().getStatistics();
        logger.info(
                "Evolutionary Algorithm phase ({}) ended: time spent ({}), best score ({}), best generation ({}), best iteration ({}), generation total ({}), iteration total ({}), overconstrained ({}).",
                phaseScope.getPhaseIndex(), phaseScope.calculateSolverTimeMillisSpentUpToNow(), phaseScope.getBestScore().raw(),
                statistics.bestGeneration(), statistics.bestIteration(), statistics.generationCount(),
                statistics.individualCount(), isComplex);
    }

    @Override
    public void stepStarted(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        evolutionaryDecider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        evolutionaryDecider.stepEnded(stepScope);
        var solver = stepScope.getPhaseScope().getSolverScope().getSolver();
        solver.getBestSolutionRecaller().processWorkingSolutionDuringStep(stepScope);
    }

    @NullMarked
    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final EvolutionaryDecider<Solution_, ?> evolutionaryDecider;
        private final boolean isComplex;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                EvolutionaryDecider<Solution_, ?> evolutionaryDecider, boolean isComplex) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.evolutionaryDecider = evolutionaryDecider;
            this.isComplex = isComplex;
        }

        @Override
        public DefaultEvolutionaryAlgorithmPhase<Solution_> build() {
            return new DefaultEvolutionaryAlgorithmPhase<>(this);
        }
    }
}
