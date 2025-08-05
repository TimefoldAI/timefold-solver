package ai.timefold.solver.core.impl.heuristic.selector.value.mimic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class MimicReplayingValueSelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    protected final ValueMimicRecorder<Solution_> valueMimicRecorder;

    protected boolean hasRecordingCreated;
    protected boolean hasRecording;
    protected boolean recordingCreated;
    protected Object recording;
    protected boolean recordingAlreadyReturned;

    public MimicReplayingValueSelector(ValueMimicRecorder<Solution_> valueMimicRecorder) {
        this.valueMimicRecorder = valueMimicRecorder;
        // No PhaseLifecycleSupport because the MimicRecordingValueSelector is hooked up elsewhere too
        valueMimicRecorder.addMimicReplayingValueSelector(this);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        valueMimicRecorder.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        valueMimicRecorder.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        valueMimicRecorder.phaseStarted(phaseScope);
        // Doing this in phaseStarted instead of stepStarted due to QueuedValuePlacer compatibility
        hasRecordingCreated = false;
        recordingCreated = false;
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        valueMimicRecorder.phaseEnded(phaseScope);
        // Doing this in phaseEnded instead of stepEnded due to QueuedValuePlacer compatibility
        hasRecordingCreated = false;
        hasRecording = false;
        recordingCreated = false;
        recording = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        valueMimicRecorder.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        valueMimicRecorder.stepEnded(stepScope);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return valueMimicRecorder.getVariableDescriptor();
    }

    @Override
    public boolean isCountable() {
        return valueMimicRecorder.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return valueMimicRecorder.isNeverEnding();
    }

    @Override
    public long getSize(Object entity) {
        return valueMimicRecorder.getSize(entity);
    }

    @Override
    public long getSize() {
        return valueMimicRecorder.getSize();
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        // Ignores the entity, but the constructor of this class guarantees that the valueRange is entity independent
        return new ReplayingValueIterator();
    }

    @Override
    public Iterator<Object> iterator() {
        return new ReplayingValueIterator();
    }

    public void recordedHasNext(boolean hasNext) {
        hasRecordingCreated = true;
        hasRecording = hasNext;
        recordingCreated = false;
        recording = null;
        recordingAlreadyReturned = false;
    }

    public void recordedNext(Object next) {
        hasRecordingCreated = true;
        hasRecording = true;
        recordingCreated = true;
        recording = next;
        recordingAlreadyReturned = false;
    }

    private class ReplayingValueIterator extends SelectionIterator<Object> {

        private ReplayingValueIterator() {
            // Reset so the last recording plays again even if it has already played
            recordingAlreadyReturned = false;
        }

        @Override
        public boolean hasNext() {
            if (!hasRecordingCreated) {
                throw new IllegalStateException("Replay must occur after record."
                        + " The recordingValueSelector (" + valueMimicRecorder
                        + ")'s hasNext() has not been called yet. ");
            }
            return hasRecording && !recordingAlreadyReturned;
        }

        @Override
        public Object next() {
            if (!recordingCreated) {
                throw new IllegalStateException("Replay must occur after record."
                        + " The recordingValueSelector (" + valueMimicRecorder
                        + ")'s next() has not been called yet. ");
            }
            if (recordingAlreadyReturned) {
                throw new NoSuchElementException("The recordingAlreadyReturned (" + recordingAlreadyReturned
                        + ") is impossible. Check if hasNext() returns true before this call.");
            }
            // Until the recorder records something, this iterator has no next.
            recordingAlreadyReturned = true;
            return recording;
        }

        @Override
        public String toString() {
            if (hasRecordingCreated && !hasRecording) {
                return "No next replay";
            }
            return "Next replay (" + (recordingCreated ? recording : "?") + ")";
        }

    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        // No replaying, because the endingIterator() is used for determining size
        return valueMimicRecorder.endingIterator(entity);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        MimicReplayingValueSelector<?> that = (MimicReplayingValueSelector<?>) other;
        return Objects.equals(valueMimicRecorder, that.valueMimicRecorder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueMimicRecorder);
    }

    @Override
    public String toString() {
        return "Replaying(" + valueMimicRecorder + ")";
    }

}
