package ai.timefold.solver.core.api.domain.specification;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Describes a value range provider on either the solution or an entity.
 *
 * @param id the value range id (null for anonymous/type-matched)
 * @param getter reads the value range from the owner
 * @param ownerClass the class that owns this value range (solution or entity class)
 * @param isEntityScoped true if this value range is on an entity rather than the solution
 * @param genericReturnType the generic return type of the value range provider (e.g., {@code List<Integer>})
 * @param <S> the solution type
 */
public record ValueRangeSpecification<S>(
        String id,
        Function<?, ?> getter,
        Class<?> ownerClass,
        boolean isEntityScoped,
        Type genericReturnType) {

    /**
     * Backwards-compatible constructor for the programmatic API.
     */
    public ValueRangeSpecification(String id, Function<?, ?> getter, Class<?> ownerClass, boolean isEntityScoped) {
        this(id, getter, ownerClass, isEntityScoped, null);
    }
}
