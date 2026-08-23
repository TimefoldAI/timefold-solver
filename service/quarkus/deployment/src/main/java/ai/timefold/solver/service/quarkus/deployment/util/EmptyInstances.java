package ai.timefold.solver.service.quarkus.deployment.util;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

/**
 * Creates "empty" instances of user model types for build-time processing.
 * <p>
 * Regular classes are instantiated through their no-arg constructor.
 * Records are instantiated through their canonical constructor with default component values
 * ({@code null} for reference types, zero/{@code false} for primitives), so models do not need
 * an all-null no-arg constructor solely to satisfy reflective instantiation.
 */
public final class EmptyInstances {

    private EmptyInstances() {
    }

    /**
     * Creates an empty instance of {@code type}.
     *
     * @param type the type to instantiate; never null
     * @return a new empty instance; never null
     * @throws ReflectiveOperationException if reflective construction fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T of(Class<T> type) throws ReflectiveOperationException {
        if (type.isRecord()) {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] paramTypes = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);
            Object[] args = Arrays.stream(components)
                    .map(component -> defaultValueFor(component.getType()))
                    .toArray();
            return type.getDeclaredConstructor(paramTypes).newInstance(args);
        }
        return type.getDeclaredConstructor().newInstance();
    }

    static Object defaultValueFor(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == char.class) {
            return '\0';
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
    }
}
