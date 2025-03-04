package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class UnimprovedStepCountTermination<Solution_>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final int unimprovedStepCountLimit;

    public UnimprovedStepCountTermination(int unimprovedStepCountLimit) {
        this.unimprovedStepCountLimit = unimprovedStepCountLimit;
        if (unimprovedStepCountLimit < 0) {
            throw new IllegalArgumentException("The unimprovedStepCountLimit (%d) cannot be negative."
                    .formatted(unimprovedStepCountLimit));
        }
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        var unimprovedStepCount = calculateUnimprovedStepCount(phaseScope);
        return unimprovedStepCount >= unimprovedStepCountLimit;
    }

    private static int calculateUnimprovedStepCount(AbstractPhaseScope<?> phaseScope) {
        var bestStepIndex = phaseScope.getBestSolutionStepIndex();
        var lastStepIndex = phaseScope.getLastCompletedStepScope().getStepIndex();
        return lastStepIndex - bestStepIndex;
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var unimprovedStepCount = calculateUnimprovedStepCount(phaseScope);
        var timeGradient = unimprovedStepCount / ((double) unimprovedStepCountLimit);
        return Math.min(timeGradient, 1.0);
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new UnimprovedStepCountTermination<>(unimprovedStepCountLimit);
    }

    @Override
    public boolean isApplicableTo(Class<? extends AbstractPhaseScope> phaseScopeClass) {
        return !(phaseScopeClass == ConstructionHeuristicPhaseScope.class
                || phaseScopeClass == CustomPhaseScope.class);
    }

    @Override
    public String toString() {
        return "UnimprovedStepCount(" + unimprovedStepCountLimit + ")";
    }

}
