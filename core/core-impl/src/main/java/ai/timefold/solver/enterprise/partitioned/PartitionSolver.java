package ai.timefold.solver.enterprise.partitioned;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.ProblemFactChange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class PartitionSolver<Solution_> extends AbstractSolver<Solution_> {

    final SolverScope<Solution_> solverScope;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public PartitionSolver(BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination<Solution_> termination,
            List<Phase<Solution_>> phaseList, SolverScope<Solution_> solverScope) {
        super(bestSolutionRecaller, termination, phaseList);
        this.solverScope = solverScope;
    }

    // ************************************************************************
    // Complex getters
    // ************************************************************************

    @Override
    public boolean isSolving() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean terminateEarly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminateEarly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addProblemFactChange(ProblemFactChange<Solution_> problemFactChange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addProblemFactChanges(List<ProblemFactChange<Solution_>> problemFactChanges) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProblemChange(ProblemChange<Solution_> problemChange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProblemChanges(List<ProblemChange<Solution_>> problemChangeList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEveryProblemChangeProcessed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEveryProblemFactChangeProcessed() {
        throw new UnsupportedOperationException();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Solution_ solve(Solution_ problem) {
        solverScope.initializeYielding();
        try {
            solverScope.setBestSolution(problem);
            solvingStarted(solverScope);
            runPhases(solverScope);
            solvingEnded(solverScope);
            return solverScope.getBestSolution();
        } finally {
            solverScope.destroyYielding();
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        solverScope.setWorkingSolutionFromBestSolution();
        super.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        solverScope.getScoreDirector().close();
        // TODO log?
    }

    public long getScoreCalculationCount() {
        return solverScope.getScoreCalculationCount();
    }

}
