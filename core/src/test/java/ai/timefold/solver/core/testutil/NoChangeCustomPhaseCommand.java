package ai.timefold.solver.core.testutil;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.api.solver.phase.PhaseCommandContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class NoChangeCustomPhaseCommand implements PhaseCommand<Object> {

    @Override
    public void changeWorkingSolution(PhaseCommandContext<Object> context) {

    }

}
