package ai.timefold.solver.core.preview.api.move.stream;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.move.MoveConstructor;

/**
 * Implement this to provide a definition for one move type.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@FunctionalInterface
public interface MoveProvider<Solution_>
        extends Function<MoveStreams<Solution_>, MoveConstructor<Solution_>> {
}
