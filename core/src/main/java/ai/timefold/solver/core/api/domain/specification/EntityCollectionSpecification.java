package ai.timefold.solver.core.api.domain.specification;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Describes an entity collection property on a planning solution.
 *
 * @param name the property name
 * @param getter reads the entity collection from the solution
 * @param setter writes the entity collection to the solution (may be null)
 * @param isSingular true if this is a singular {@code @PlanningEntityProperty} (not a collection)
 * @param <S> the solution type
 */
public record EntityCollectionSpecification<S>(
        String name,
        Function<S, ? extends Collection<?>> getter,
        @Nullable BiConsumer<S, Object> setter,
        boolean isSingular) {

    /**
     * Constructor without setter (programmatic API, always treated as collection).
     */
    public EntityCollectionSpecification(String name, Function<S, ? extends Collection<?>> getter) {
        this(name, getter, null, false);
    }

    /**
     * Constructor without setter (annotation path).
     */
    public EntityCollectionSpecification(String name, Function<S, ? extends Collection<?>> getter, boolean isSingular) {
        this(name, getter, null, isSingular);
    }
}
