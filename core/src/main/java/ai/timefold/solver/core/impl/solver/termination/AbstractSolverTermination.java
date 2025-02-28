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

    protected static <Solution_> void solvingStarted(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingStarted(scope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    protected static <Solution_> void solvingEnded(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingEnded(scope);
        }
    }

}
