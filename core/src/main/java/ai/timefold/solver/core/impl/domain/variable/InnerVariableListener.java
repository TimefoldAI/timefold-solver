package ai.timefold.solver.core.impl.domain.variable;

import java.io.Closeable;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * A listener sourced on a basic {@link PlanningVariable}.
 * <p>
 * Changes shadow variables when a source basic planning variable changes.
 * The source variable can be either a genuine or a shadow variable.
 * <p>
 * It is recommended to keep implementations stateless.
 * If state must be implemented, implementations may need to override the default methods
 * ({@link #resetWorkingSolution(InnerScoreDirector)}, {@link #close()}).
 * 
 * @param <Solution_>
 * @param <ChangeEvent_>
 */
@NullMarked
public interface InnerVariableListener<Solution_, ChangeEvent_ extends ChangeEvent> extends Closeable {

    void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector, ChangeEvent_ event);

    void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector, ChangeEvent_ event);

    /**
     * When set to {@code true}, this has a performance loss.
     * When set to {@code false}, it's easier to make the listener implementation correct and fast.
     *
     * @return true to guarantee that each of the before/after methods is only called once per entity instance
     *         per operation type (add, change or remove).
     */
    default boolean requiresUniqueEntityEvents() {
        return false;
    }

    /**
     * Called when the entire working solution changes. In this event, the other before..()/after...() methods will not
     * be called.
     * At this point, implementations should clear state, if any.
     */
    default void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        // No need to do anything for stateless implementations.
    }

    /**
     * Called before this {@link InnerVariableListener} is thrown away and not used anymore.
     */
    @Override
    default void close() {
        // No need to do anything for stateless implementations.
    }

    @SuppressWarnings("unchecked")
    static <Solution_, Entity_> void forEachEntity(InnerScoreDirector<Solution_, ?> scoreDirector,
            Class<? extends Entity_> entityClass,
            Consumer<Entity_> entityConsumer) {
        scoreDirector.getSolutionDescriptor().visitEntitiesByEntityClass(scoreDirector.getWorkingSolution(),
                entityClass, entity -> {
                    entityConsumer.accept((Entity_) entity);
                    return false;
                });
    }
}
