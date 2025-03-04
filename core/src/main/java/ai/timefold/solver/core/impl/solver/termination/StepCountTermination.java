package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class StepCountTermination<Solution_>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final int stepCountLimit;

    public StepCountTermination(int stepCountLimit) {
        this.stepCountLimit = stepCountLimit;
        if (stepCountLimit < 0) {
            throw new IllegalArgumentException("The stepCountLimit (" + stepCountLimit
                    + ") cannot be negative.");
        }
    }

    public int getStepCountLimit() {
        return stepCountLimit;
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        int nextStepIndex = phaseScope.getNextStepIndex();
        return nextStepIndex >= stepCountLimit;
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        int nextStepIndex = phaseScope.getNextStepIndex();
        double timeGradient = nextStepIndex / ((double) stepCountLimit);
        return Math.min(timeGradient, 1.0);
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new StepCountTermination<>(stepCountLimit);
    }

    @Override
    public String toString() {
        return "StepCount(" + stepCountLimit + ")";
    }

}
