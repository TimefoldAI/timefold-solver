package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public record FieldHandle(Field field, MethodHandle getter, MethodHandle setter) {

    public static FieldHandle of(Field field) {
        try {
            field.setAccessible(true);
            var getter = MethodHandles.lookup().unreflectGetter(field);
            var setter = MethodHandles.lookup().unreflectSetter(field);
            return new FieldHandle(field, getter, setter);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "The field (%s) cannot be accessed to create a planning clone."
                            .formatted(field),
                    e);
        }
    }

    public Object get(Object bean) {
        try { // Don't null-check the bean; if null, it fails anyway.
            return getter.invoke(bean);
        } catch (Throwable e) {
            var beanClass = bean == null ? "null" : bean.getClass();
            throw new IllegalStateException(
                    "Cannot get the field (%s) on bean of class (%s)."
                            .formatted(field.getName(), beanClass),
                    e);
        }
    }

    public void set(Object bean, Object value) {
        try { // Don't null-check the bean; if null, it fails anyway.
            setter.invoke(bean, value);
        } catch (Throwable e) {
            var beanClass = bean == null ? "null" : bean.getClass();
            throw new IllegalStateException(
                    "Cannot set the field (%s) on bean of class (%s)."
                            .formatted(field.getName(), beanClass),
                    e);
        }
    }

}
