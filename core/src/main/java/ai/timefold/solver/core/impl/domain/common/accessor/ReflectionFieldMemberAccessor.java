package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * A {@link MemberAccessor} based on a field.
 */
public final class ReflectionFieldMemberAccessor extends AbstractMemberAccessor {

    private final FieldHandle fieldHandle;

    public ReflectionFieldMemberAccessor(Field field) {
        this.fieldHandle = FieldHandle.of(field); // Use MethodHandles to access the field.
    }

    @Override
    public Class<?> getDeclaringClass() {
        return fieldHandle.field().getDeclaringClass();
    }

    @Override
    public String getName() {
        return fieldHandle.field().getName();
    }

    @Override
    public Class<?> getType() {
        return fieldHandle.field().getType();
    }

    @Override
    public Type getGenericType() {
        return fieldHandle.field().getGenericType();
    }

    @Override
    public Object executeGetter(Object bean) {
        var field = fieldHandle.field();
        if (bean == null) {
            throw new IllegalArgumentException("Requested field (%s) on a null bean."
                    .formatted(field));
        }
        return fieldHandle.get(bean);
    }

    @Override
    public boolean supportSetter() {
        return true;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        var field = fieldHandle.field();
        if (bean == null) {
            throw new IllegalArgumentException("Requested field (%s) on a null bean."
                    .formatted(field));
        }
        fieldHandle.set(bean, value);
    }

    @Override
    public String getSpeedNote() {
        return "reflection";
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return fieldHandle.field().getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return fieldHandle.field().getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public String toString() {
        return "field " + fieldHandle.field();
    }

}
