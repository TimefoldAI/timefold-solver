package ai.timefold.solver.core.impl.solver.termination;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * See {@link TerminationId} for the id model.
 *
 * @param nullablePhaseIndex null if the termination is configured on the solver
 */
@NullMarked
public record DefaultTerminationId(TerminationType type, @Nullable TerminationId parent, int ordinal,
        @Nullable Integer nullablePhaseIndex)
        implements
            TerminationId {

    @Override
    public OptionalInt phaseIndex() {
        return nullablePhaseIndex == null ? OptionalInt.empty() : OptionalInt.of(nullablePhaseIndex);
    }

}
