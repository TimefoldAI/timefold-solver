package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * A {@link MemberAccessor} based on a single read {@link Method}.
 * Do not confuse with {@link ReflectionBeanPropertyMemberAccessor} which is richer.
 */
public final class ReflectionMethodMemberAccessor extends AbstractMemberAccessor {

    private final Class<?> returnType;
    private final String methodName;
    private final Method readMethod;
    private final MethodHandle methodHandle;

    public ReflectionMethodMemberAccessor(Method readMethod) {
        this(readMethod, true);
    }

    public ReflectionMethodMemberAccessor(Method readMethod, boolean returnTypeRequired) {
        this.readMethod = readMethod;
        this.returnType = readMethod.getReturnType();
        this.methodName = readMethod.getName();
        try {
            readMethod.setAccessible(true);
            this.methodHandle = MethodHandles.lookup()
                    .unreflect(readMethod)
                    .asFixedArity();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("""
                    Impossible state: Method (%s) not accessible.
                    %s
                    """.formatted(readMethod, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
        }
        if (readMethod.getParameterTypes().length != 0) {
            throw new IllegalArgumentException("The readMethod (" + readMethod + ") must not have any parameters ("
                    + Arrays.toString(readMethod.getParameterTypes()) + ").");
        }
        if (returnTypeRequired && readMethod.getReturnType() == void.class) {
            throw new IllegalArgumentException("The readMethod (" + readMethod + ") must have a return type ("
                    + readMethod.getReturnType() + ").");
        }
    }

    @Override
    public Class<?> getDeclaringClass() {
        return readMethod.getDeclaringClass();
    }

    @Override
    public String getName() {
        return methodName;
    }

    @Override
    public Class<?> getType() {
        return returnType;
    }

    @Override
    public Type getGenericType() {
        return readMethod.getGenericReturnType();
    }

    @Override
    public Object executeGetter(Object bean) {
        try {
            return methodHandle.invoke(bean);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) getterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(methodName, readMethod, bean.getClass()), e);
        }
    }

    @Override
    public String getSpeedNote() {
        return "MethodHandle";
    }

    @Override
    public boolean supportSetter() {
        return false;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return readMethod.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return readMethod.getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public String toString() {
        return "method " + methodName + " on " + readMethod.getDeclaringClass();
    }

}
