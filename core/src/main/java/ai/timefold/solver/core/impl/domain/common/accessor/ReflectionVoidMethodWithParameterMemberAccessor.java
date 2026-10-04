package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Invokes a public void method that takes exactly one argument.
 */
public final class ReflectionVoidMethodWithParameterMemberAccessor extends AbstractMemberAccessor {

    private final Method method;
    private final MethodHandle methodHandle;
    private final String methodName;
    private final Type parameterType;

    public ReflectionVoidMethodWithParameterMemberAccessor(Method method) {
        this.method = method;
        this.methodName = method.getName();
        this.parameterType = method.getGenericParameterTypes()[0];
        try {
            this.methodHandle = MethodHandles.lookup()
                    .unreflect(method)
                    .asFixedArity();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("""
                    Impossible state: Method (%s) not accessible.
                    %s
                    """.formatted(method, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
        }
    }

    @Override
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    @Override
    public String getName() {
        return methodName;
    }

    @Override
    public Class<?> getType() {
        return method.getReturnType();
    }

    @Override
    public Type getGenericType() {
        return method.getGenericReturnType();
    }

    @Override
    public Type getGetterMethodParameterType() {
        return parameterType;
    }

    @Override
    public Object executeGetter(Object bean) {
        throw new UnsupportedOperationException(
                "The method (%s) requires a parameter. Maybe call executeGetter(Object, Object) instead."
                        .formatted(method));
    }

    @Override
    public Object executeGetter(Object bean, Object value) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested method (%s) on a null bean.".formatted(method));
        }
        try {
            methodHandle.invoke(bean, value);
        } catch (Throwable e) {
            throw new IllegalStateException("The method (%s) on bean of class (%s) throws an exception."
                    .formatted(method, bean.getClass()), e);
        }
        return null;
    }

    @Override
    public boolean supportSetter() {
        return false;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        throw new UnsupportedOperationException(
                "The method (%s) is invoked with executeGetter(Object, Object).".formatted(method));
    }

    @Override
    public String getSpeedNote() {
        return "MethodHandle";
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return method.getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public String toString() {
        return "method " + methodName + " on " + method.getDeclaringClass();
    }

}
