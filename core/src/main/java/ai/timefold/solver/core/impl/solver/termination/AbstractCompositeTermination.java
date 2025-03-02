package ai.timefold.solver.core.impl.solver.termination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    protected final List<PhaseTermination<Solution_>> phaseTerminationList;
    protected final List<SolverTermination<Solution_>> solverTerminationList;

    protected AbstractCompositeTermination(List<Termination<Solution_>> terminationList) {
        this.terminationList = Objects.requireNonNull(terminationList);
        this.phaseTerminationList = terminationList.stream()
                .filter(PhaseTermination.class::isInstance)
                .map(t -> (PhaseTermination<Solution_>) t)
                .toList();
        this.solverTerminationList = terminationList.stream()
                .filter(SolverTermination.class::isInstance)
                .map(t -> (SolverTermination<Solution_>) t)
                .toList();
    }

    @SafeVarargs
    public AbstractCompositeTermination(Termination<Solution_>... terminations) {
        this(Arrays.asList(terminations));
    }

    @Override
    public final void solvingStarted(SolverScope<Solution_> solverScope) {
        for (var termination : solverTerminationList) {
            termination.solvingStarted(solverScope);
        }
    }

    @Override
    public final void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : phaseTerminationList) {
            termination.phaseStarted(phaseScope);
        }
    }

    @Override
    public final void stepStarted(AbstractStepScope<Solution_> stepScope) {
        for (var termination : phaseTerminationList) {
            termination.stepStarted(stepScope);
        }
    }

    @Override
    public final void stepEnded(AbstractStepScope<Solution_> stepScope) {
        for (var termination : phaseTerminationList) {
            termination.stepEnded(stepScope);
        }
    }

    @Override
    public final void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : phaseTerminationList) {
            termination.phaseEnded(phaseScope);
        }
    }

    @Override
    public final void solvingEnded(SolverScope<Solution_> solverScope) {
        for (var termination : solverTerminationList) {
            termination.solvingEnded(solverScope);
        }
    }

    protected final List<Termination<Solution_>> createChildThreadTerminationList(SolverScope<Solution_> solverScope,
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
    public final List<PhaseTermination<Solution_>> getPhaseTerminationList() {
        var result = new ArrayList<PhaseTermination<Solution_>>();
        for (var termination : phaseTerminationList) {
            result.add(termination);
            if (termination instanceof UniversalTermination<Solution_> universalTermination) {
                result.addAll(universalTermination.getPhaseTerminationList());
            }
        }
        return List.copyOf(result);
    }

}
