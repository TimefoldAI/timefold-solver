package ai.timefold.solver.core.api.domain.specification;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Describes a problem fact or problem fact collection on a planning solution.
 *
 * @param name the property name
 * @param getter reads the fact(s) from the solution
 * @param setter writes the fact(s) to the solution (may be null for annotation path without cloning spec)
 * @param isCollection true if this is a collection of facts
 * @param genericType the generic return type of the member (e.g., {@code List<MyFact>})
 * @param <S> the solution type
 */
public record FactSpecification<S>(
        String name,
        Function<S, ?> getter,
        @Nullable BiConsumer<S, Object> setter,
        boolean isCollection,
        Type genericType) {

    /**
     * Constructor without setter or generic type (programmatic API).
     */
    public FactSpecification(String name, Function<S, ?> getter, boolean isCollection) {
        this(name, getter, null, isCollection, null);
    }

    /**
     * Constructor without setter (annotation path where setter is resolved later if needed).
     */
    public FactSpecification(String name, Function<S, ?> getter, boolean isCollection, Type genericType) {
        this(name, getter, null, isCollection, genericType);
    }
}
