package ai.timefold.solver.core.impl.solver.monitoring.statistic;

import java.util.Map;
import java.util.WeakHashMap;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.solution.mutation.MutationCounter;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

import org.jspecify.annotations.NonNull;

import io.micrometer.core.instrument.Metrics;

public class BestSolutionMutationCountStatistic<Solution_> implements SolverStatistic<Solution_> {

    private final Map<Solver<Solution_>, SolverEventListener<Solution_>> solverToEventListenerMap = new WeakHashMap<>();

    @Override
    public void unregister(Solver<Solution_> solver) {
        SolverEventListener<Solution_> listener = solverToEventListenerMap.remove(solver);
        if (listener != null) {
            solver.removeEventListener(listener);
        }
    }

    @Override
    public void register(Solver<Solution_> solver) {
        DefaultSolver<Solution_> defaultSolver = (DefaultSolver<Solution_>) solver;
        ScoreDirectorFactory<Solution_, ?> scoreDirectorFactory = defaultSolver.getScoreDirectorFactory();
        SolutionDescriptor<Solution_> solutionDescriptor = scoreDirectorFactory.getSolutionDescriptor();
        MutationCounter<Solution_> mutationCounter = new MutationCounter<>(solutionDescriptor);
        BestSolutionMutationCountStatisticListener<Solution_> listener =
                Metrics.gauge(SolverMetric.BEST_SOLUTION_MUTATION.getMeterId(),
                        defaultSolver.getSolverScope().getMonitoringTags(),
                        new BestSolutionMutationCountStatisticListener<>(mutationCounter),
                        BestSolutionMutationCountStatisticListener::getMutationCount);
        solverToEventListenerMap.put(solver, listener);
        solver.addEventListener(listener);
    }

    private static class BestSolutionMutationCountStatisticListener<Solution_> implements SolverEventListener<Solution_> {
        final MutationCounter<Solution_> mutationCounter;
        int mutationCount = 0;
        Solution_ oldBestSolution = null;

        public BestSolutionMutationCountStatisticListener(MutationCounter<Solution_> mutationCounter) {
            this.mutationCounter = mutationCounter;
        }

        public int getMutationCount() {
            return mutationCount;
        }

        @Override
        public void bestSolutionChanged(@NonNull BestSolutionChangedEvent<Solution_> event) {
            Solution_ newBestSolution = event.getNewBestSolution();
            if (oldBestSolution == null) {
                mutationCount = 0;
            } else {
                mutationCount = mutationCounter.countMutations(oldBestSolution, newBestSolution);
            }
            oldBestSolution = newBestSolution;
        }
    }
}
