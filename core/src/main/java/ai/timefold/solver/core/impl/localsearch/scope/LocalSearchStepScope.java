package ai.timefold.solver.core.impl.localsearch.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class LocalSearchStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final LocalSearchPhaseScope<Solution_> phaseScope;

    private double timeGradient = Double.NaN;
    private Move<Solution_> step = null;
    private String stepString = null;
    private Long selectedMoveCount = null;
    private Long acceptedMoveCount = null;

    public LocalSearchStepScope(LocalSearchPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex());
    }

    public LocalSearchStepScope(LocalSearchPhaseScope<Solution_> phaseScope, int stepIndex) {
        super(stepIndex);
        this.phaseScope = phaseScope;
    }

    @Override
    public LocalSearchPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    public double getTimeGradient() {
        return timeGradient;
    }

    public void setTimeGradient(double timeGradient) {
        this.timeGradient = timeGradient;
    }

    public Move<Solution_> getStep() {
        return step;
    }

    public void setStep(Move<Solution_> step) {
        this.step = step;
    }

    /**
     * @return null if logging level is too high
     */
    public String getStepString() {
        return stepString;
    }

    public void setStepString(String stepString) {
        this.stepString = stepString;
    }

    public Long getSelectedMoveCount() {
        return selectedMoveCount;
    }

    public void setSelectedMoveCount(Long selectedMoveCount) {
        this.selectedMoveCount = selectedMoveCount;
    }

    public Long getAcceptedMoveCount() {
        return acceptedMoveCount;
    }

    public void setAcceptedMoveCount(Long acceptedMoveCount) {
        this.acceptedMoveCount = acceptedMoveCount;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

}
