package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;

/**
 * A {@link MemberAccessor} based on a getter and optionally a setter.
 */
public final class ReflectionBeanPropertyMemberAccessor extends AbstractMemberAccessor {

    private final Class<?> propertyType;
    private final String propertyName;
    private final Method getterMethod;
    private final MethodHandle getherMethodHandle;
    private final Method setterMethod;
    private final MethodHandle setterMethodHandle;
    private final AnnotatedElement annotatedElement;

    public ReflectionBeanPropertyMemberAccessor(Method getterMethod) {
        this(getterMethod, getterMethod, false);
    }

    public ReflectionBeanPropertyMemberAccessor(Method getterMethod, AnnotatedElement annotatedElement, boolean getterOnly) {
        this.getterMethod = getterMethod;
        this.annotatedElement = annotatedElement;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            this.getterMethod.setAccessible(true);
            this.getherMethodHandle = lookup.unreflect(getterMethod)
                    .asFixedArity();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("""
                    Impossible state: method (%s) not accessible.
                    %s"""
                    .formatted(getterMethod, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
        }
        Class<?> declaringClass = getterMethod.getDeclaringClass();
        propertyType = getterMethod.getReturnType();
        propertyName = ReflectionHelper.getGetterPropertyName(getterMethod);
        if (getterOnly) {
            setterMethod = null;
            setterMethodHandle = null;
        } else {
            setterMethod = ReflectionHelper.getDeclaredSetterMethod(declaringClass, getterMethod.getReturnType(), propertyName);
            try {
                this.setterMethod.setAccessible(true);
                this.setterMethodHandle = lookup.unreflect(setterMethod)
                        .asFixedArity();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("""
                        Impossible state: method (%s) not accessible.
                        %s"""
                        .formatted(setterMethod, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
            }
        }
    }

    @Override
    public Class<?> getDeclaringClass() {
        return getterMethod.getDeclaringClass();
    }

    @Override
    public String getName() {
        return propertyName;
    }

    @Override
    public Class<?> getType() {
        return propertyType;
    }

    @Override
    public Type getGenericType() {
        return getterMethod.getGenericReturnType();
    }

    @Override
    public Object executeGetter(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested property (%s) getterMethod (%s) on a null bean."
                    .formatted(propertyName, getterMethod));
        }
        try {
            return getherMethodHandle.invoke(bean);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) getterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(propertyName, getterMethod, bean.getClass()), e);
        }
    }

    @Override
    public boolean supportSetter() {
        return setterMethod != null;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested property (%s) setterMethod (%s) on a null bean."
                    .formatted(propertyName, setterMethod));
        }
        try {
            setterMethodHandle.invoke(bean, value);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) setterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(propertyName, setterMethod, bean.getClass()), e);
        }
    }

    @Override
    public String getSpeedNote() {
        return "MethodHandle";
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return annotatedElement.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return annotatedElement.getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ReflectionBeanPropertyMemberAccessor that))
            return false;
        return Objects.equals(propertyType, that.propertyType) && Objects.equals(propertyName,
                that.propertyName) && Objects.equals(annotatedElement, that.annotatedElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyType, propertyName, annotatedElement);
    }

    @Override
    public String toString() {
        return "bean property " + propertyName + " on " + getterMethod.getDeclaringClass();
    }

}
