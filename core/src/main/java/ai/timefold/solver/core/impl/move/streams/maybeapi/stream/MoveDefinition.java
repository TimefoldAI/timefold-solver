package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Implement this to provide a definition for one move type.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface MoveDefinition<Solution_> {

    MoveProducer<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory);

}
