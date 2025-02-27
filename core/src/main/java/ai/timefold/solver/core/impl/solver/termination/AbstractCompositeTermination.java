package ai.timefold.solver.core.impl.solver.termination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

/**
 * Abstract superclass that combines multiple {@link Termination}s.
 *
 * @see AndCompositeTermination
 * @see OrCompositeTermination
 */
abstract sealed class AbstractCompositeTermination<Solution_>
        extends AbstractSolverTermination<Solution_>
        permits AndCompositeTermination, OrCompositeTermination {

    protected final List<Termination<Solution_>> terminationList;

    protected AbstractCompositeTermination(List<Termination<Solution_>> terminationList) {
        this.terminationList = terminationList;
    }

    public AbstractCompositeTermination(Termination<Solution_>... terminations) {
        this(Arrays.asList(terminations));
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        for (Termination<Solution_> termination : terminationList) {
            solvingStarted(termination, solverScope);
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        for (Termination<Solution_> termination : terminationList) {
            phaseStarted(termination, phaseScope);
        }
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        for (Termination<Solution_> termination : terminationList) {
            stepStarted(termination, stepScope);
        }
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        for (Termination<Solution_> termination : terminationList) {
            stepEnded(termination, stepScope);
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        for (Termination<Solution_> termination : terminationList) {
            phaseEnded(termination, phaseScope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        for (Termination<Solution_> termination : terminationList) {
            solvingEnded(termination, solverScope);
        }
    }

    protected List<Termination<Solution_>> createChildThreadTerminationList(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        List<Termination<Solution_>> childThreadTerminationList = new ArrayList<>(terminationList.size());
        for (Termination<Solution_> termination : terminationList) {
            var childThreadSupportingTermination = ChildThreadSupportingTermination.assertChildThreadSupport(termination);
            childThreadTerminationList
                    .add(childThreadSupportingTermination.createChildThreadTermination(solverScope, childThreadType));
        }
        return childThreadTerminationList;
    }

}
