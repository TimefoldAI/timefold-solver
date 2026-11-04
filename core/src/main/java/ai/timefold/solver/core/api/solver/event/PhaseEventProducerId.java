package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.api.solver.phase.PhaseId;

import org.jspecify.annotations.NullMarked;

/**
 * {@link EventProducerId} for when a {@link BestSolutionChangedEvent} is caused by a phase.
 */
@NullMarked
public interface PhaseEventProducerId extends EventProducerId {

    /**
     * @return the phase that produced the event
     */
    PhaseId phaseId();

}
