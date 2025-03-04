package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractSolverTermination<Solution_>
        extends AbstractTermination<Solution_>
        implements SolverTermination<Solution_> permits BasicPlumbingTermination, ChildThreadPlumbingTermination {

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

}
