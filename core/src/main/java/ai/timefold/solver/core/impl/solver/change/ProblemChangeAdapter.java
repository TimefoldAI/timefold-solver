package ai.timefold.solver.core.impl.solver.change;

import ai.timefold.solver.core.api.solver.ProblemFactChange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Provides a layer of abstraction over {@link ai.timefold.solver.core.api.solver.change.ProblemChange} and the
 * deprecated {@link ai.timefold.solver.core.api.solver.ProblemFactChange} to preserve backward compatibility.
 */
public interface ProblemChangeAdapter<Solution_> {

    void doProblemChange(SolverScope<Solution_> solverScope);

    static <Solution_> ProblemChangeAdapter<Solution_> create(ProblemFactChange<Solution_> problemFactChange) {
        return (solverScope) -> problemFactChange.doChange(solverScope.getScoreDirector());
    }

    static <Solution_> ProblemChangeAdapter<Solution_> create(ProblemChange<Solution_> problemChange) {
        return (solverScope) -> {
            problemChange.doChange(solverScope.getWorkingSolution(), solverScope.getProblemChangeDirector());
            solverScope.getScoreDirector().triggerVariableListeners();
        };
    }
}
