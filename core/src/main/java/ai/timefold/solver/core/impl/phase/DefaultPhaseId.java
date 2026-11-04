package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.phase.PhaseType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultPhaseId(PhaseType type, int phaseIndex) implements PhaseId {

    @Override
    public String toString() {
        return "%s (%d)".formatted(type.getPhaseName(), phaseIndex);
    }

}
