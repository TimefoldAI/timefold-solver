package ai.timefold.solver.core.api.domain.variable;

import java.io.Closeable;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NonNull;

/**
 * Common ancestor for specialized planning variable listeners.
 * <p>
 * <strong>Do not implement this interface directly.</strong>
 * Implement either {@link VariableListener} or {@link ListVariableListener}.
 *
 * @see VariableListener
 * @see ListVariableListener
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> {@link PlanningEntity} on which the source variable is declared
 */
public interface AbstractVariableListener<Solution_, Entity_> extends Closeable {

    void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity);

    void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity);

    void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity);

    void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Entity_ entity);

    /**
     * Called when the entire working solution changes. In this event, the other before..()/after...() methods will not
     * be called.
     * At this point, implementations should clear state, if any.
     */
    default void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        // No need to do anything for stateless implementations.
    }

    /**
     * Called before this {@link AbstractVariableListener} is thrown away and not used anymore.
     */
    @Override
    default void close() {
        // No need to do anything for stateless implementations.
    }
}
