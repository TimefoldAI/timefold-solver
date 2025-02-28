package ai.timefold.solver.core.impl.solver.termination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

/**
 * Abstract superclass that combines multiple {@link Termination}s.
 *
 * @see AndCompositeTermination
 * @see OrCompositeTermination
 */
@NullMarked
abstract sealed class AbstractCompositeTermination<Solution_>
        extends AbstractUniversalTermination<Solution_>
        permits AndCompositeTermination, OrCompositeTermination {

    protected final List<Termination<Solution_>> terminationList;

    protected AbstractCompositeTermination(List<Termination<Solution_>> terminationList) {
        this.terminationList = terminationList;
    }

    @SafeVarargs
    public AbstractCompositeTermination(Termination<Solution_>... terminations) {
        this(Arrays.asList(terminations));
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        for (var termination : terminationList) {
            solvingStarted(termination, solverScope);
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : terminationList) {
            phaseStarted(termination, phaseScope);
        }
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        for (var termination : terminationList) {
            stepStarted(termination, stepScope);
        }
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        for (var termination : terminationList) {
            stepEnded(termination, stepScope);
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : terminationList) {
            phaseEnded(termination, phaseScope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        for (var termination : terminationList) {
            solvingEnded(termination, solverScope);
        }
    }

    protected List<Termination<Solution_>> createChildThreadTerminationList(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        List<Termination<Solution_>> childThreadTerminationList = new ArrayList<>(terminationList.size());
        for (var termination : terminationList) {
            var childThreadSupportingTermination = ChildThreadSupportingTermination.assertChildThreadSupport(termination);
            childThreadTerminationList
                    .add(childThreadSupportingTermination.createChildThreadTermination(solverScope, childThreadType));
        }
        return childThreadTerminationList;
    }

    @Override
    public List<PhaseTermination<Solution_>> getPhaseTerminationList() {
        var phaseTerminationList = new ArrayList<PhaseTermination<Solution_>>();
        for (var termination : terminationList) {
            if (termination instanceof UniversalTermination<Solution_> universalTermination) {
                phaseTerminationList.addAll(universalTermination.getPhaseTerminationList());
            } else if (termination instanceof PhaseTermination<Solution_> phaseTermination) {
                phaseTerminationList.add(phaseTermination);
            }
        }
        return List.copyOf(phaseTerminationList);
    }

    @Override
    public List<PhaseTermination<Solution_>> getUnsupportedPhaseTerminationList(AbstractPhaseScope<Solution_> phaseScope) {
        var phaseTerminationList = new ArrayList<PhaseTermination<Solution_>>();
        for (var termination : terminationList) {
            if (termination instanceof UniversalTermination<Solution_> universalTermination) {
                phaseTerminationList.addAll(universalTermination.getUnsupportedPhaseTerminationList(phaseScope));
            } else if (termination instanceof PhaseTermination<Solution_> phaseTermination
                    && !phaseTermination.isSupported(phaseScope)) {
                phaseTerminationList.add(phaseTermination);
            }
        }
        return List.copyOf(phaseTerminationList);
    }
}
