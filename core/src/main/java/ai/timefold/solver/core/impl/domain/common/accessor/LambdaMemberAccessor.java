package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A {@link MemberAccessor} that delegates to user-provided lambda functions
 * instead of using reflection. Used by the programmatic specification API.
 * <p>
 * Annotations always return null since configuration comes from the specification, not annotations.
 */
public final class LambdaMemberAccessor extends AbstractMemberAccessor {

    private final String name;
    private final Class<?> declaringClass;
    private final Class<?> type;
    private final Type genericType;
    private final Function<Object, Object> getter;
    private final BiConsumer<Object, Object> setter;

    @SuppressWarnings("unchecked")
    public LambdaMemberAccessor(String name, Class<?> declaringClass, Class<?> type, Type genericType,
            Function<?, ?> getter, BiConsumer<?, ?> setter) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.type = type;
        this.genericType = genericType != null ? genericType : type;
        this.getter = (Function<Object, Object>) getter;
        this.setter = (BiConsumer<Object, Object>) setter;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public Object executeGetter(Object bean) {
        return getter.apply(bean);
    }

    @Override
    public boolean supportSetter() {
        return setter != null;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        if (setter == null) {
            throw new UnsupportedOperationException(
                    "The lambda member accessor '%s' on %s does not support setting."
                            .formatted(name, declaringClass.getSimpleName()));
        }
        setter.accept(bean, value);
    }

    @Override
    public String getSpeedNote() {
        return "lambda";
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return null;
    }

    @Override
    public String toString() {
        return "lambda:" + declaringClass.getSimpleName() + "." + name;
    }
}
