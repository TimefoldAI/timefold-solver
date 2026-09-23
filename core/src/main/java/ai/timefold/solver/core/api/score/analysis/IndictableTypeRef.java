package ai.timefold.solver.core.api.score.analysis;

/**
 * Represents a unique identifier of an {@link Indictable} class.
 * <p>
 * If you need an instance created, use {@link IndictableTypeRef#of(Class)} or {@link IndictableTypeRef#of(String)}
 * and not the record's constructors.
 *
 * @param id The indictable class id. It must be unique per class.
 */
public record IndictableTypeRef(String id) {
    public static IndictableTypeRef of(String id) {
        return new IndictableTypeRef(id);
    }

    public static IndictableTypeRef of(Class<?> clazz) {
        return new IndictableTypeRef(clazz.getCanonicalName());
    }
}
