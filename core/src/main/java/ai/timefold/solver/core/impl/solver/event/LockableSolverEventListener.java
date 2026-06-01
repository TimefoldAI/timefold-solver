package ai.timefold.solver.core.impl.solver.event;

import java.util.concurrent.locks.Lock;

import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;

import org.jspecify.annotations.NonNull;

public interface LockableSolverEventListener<Solution_> extends SolverEventListener<Solution_> {
    default void bestSolutionChanged(@NonNull BestSolutionChangedEvent<Solution_> event) {
        throw new UnsupportedOperationException();
    }

    void bestSolutionChanged(@NonNull BestSolutionChangedEvent<Solution_> event, @NonNull Lock lock);
}
