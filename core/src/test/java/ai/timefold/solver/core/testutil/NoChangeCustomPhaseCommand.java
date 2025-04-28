package ai.timefold.solver.core.testutil;

import java.util.function.BooleanSupplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class NoChangeCustomPhaseCommand implements PhaseCommand<Object> {

    @Override
    public void changeWorkingSolution(ScoreDirector<Object> scoreDirector, BooleanSupplier isPhaseTerminated) {
        // Do nothing
    }

}
