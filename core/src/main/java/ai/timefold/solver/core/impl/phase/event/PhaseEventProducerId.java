package ai.timefold.solver.core.impl.phase.event;

import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.phase.PhaseType;

import org.jspecify.annotations.NullMarked;

/**
 * {@link EventProducerId} for when a {@link BestSolutionChangedEvent} is
 * caused by a phase.
 */
@NullMarked
public record PhaseEventProducerId(PhaseType phaseType, int phaseIndex) implements EventProducerId {
    @Override
    public String producerId() {
        return "%s (%d)".formatted(phaseType.getPhaseName(), phaseIndex);
    }

    @Override
    public String simpleProducerName() {
        return phaseType.getPhaseName();
    }

    @Override
    public String toString() {
        return producerId();
    }
}
