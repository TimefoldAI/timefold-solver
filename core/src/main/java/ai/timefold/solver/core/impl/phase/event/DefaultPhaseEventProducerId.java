package ai.timefold.solver.core.impl.phase.event;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.event.PhaseEventProducerId;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.DefaultPhaseId;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultPhaseEventProducerId(PhaseId phaseId) implements PhaseEventProducerId {

    public static DefaultPhaseEventProducerId of(PhaseType type, int phaseIndex) {
        return new DefaultPhaseEventProducerId(new DefaultPhaseId(type, phaseIndex));
    }

    @Override
    public String producerId() {
        return phaseId.toString();
    }

    @Override
    public String simpleProducerName() {
        return phaseId.type().getPhaseName();
    }

    @Override
    public OptionalInt phaseIndex() {
        return OptionalInt.of(phaseId.phaseIndex());
    }

    @Override
    public String toString() {
        return producerId();
    }
}
