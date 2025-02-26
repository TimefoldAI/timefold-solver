package ai.timefold.solver.core.impl.phase.custom;

import java.util.function.BooleanSupplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;

import org.jspecify.annotations.NullMarked;

/**
 * @deprecated Use {@link PhaseCommand} instead.
 */
@FunctionalInterface
@NullMarked
@Deprecated(forRemoval = true, since = "1.20.0")
public interface CustomPhaseCommand<Solution_> extends PhaseCommand<Solution_> {

    @Override
    default void changeWorkingSolution(ScoreDirector<Solution_> scoreDirector, BooleanSupplier isPhaseTerminated) {
        changeWorkingSolution(scoreDirector);
    }

    void changeWorkingSolution(ScoreDirector<Solution_> scoreDirector);

}
