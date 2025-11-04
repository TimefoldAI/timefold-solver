package ai.timefold.solver.core.api.solver.event;

import java.util.OptionalInt;

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
    public OptionalInt eventPhaseIndex() {
        return OptionalInt.of(phaseIndex);
    }

    @Override
    public String toString() {
        return producerId();
    }
}
